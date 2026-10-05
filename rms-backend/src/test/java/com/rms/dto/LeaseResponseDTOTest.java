package com.rms.dto;

import com.rms.entity.Lease;
import com.rms.entity.LeaseStatus;
import com.rms.entity.Space;
import com.rms.entity.SpaceStatus;
import com.rms.entity.Tenant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LeaseResponseDTOTest {

    private static Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId(7L);
        tenant.setName("Abebe Trading");
        tenant.setPhone("0911223344");
        return tenant;
    }

    private static Space space() {
        Space space = new Space();
        space.setId(3L);
        space.setUnitNumber("G-01");
        space.setFloor(0);
        space.setCategory("Office");
        space.setSize(new BigDecimal("45.00"));
        space.setStatus(SpaceStatus.OCCUPIED);
        return space;
    }

    private static Lease lease(Tenant tenant, Space space) {
        Lease lease = new Lease();
        lease.setId(11L);
        lease.setTenant(tenant);
        lease.setSpace(space);
        lease.setStartDate(LocalDate.of(2026, 1, 1));
        lease.setEndDate(LocalDate.of(2027, 1, 1));
        lease.setMonthlyRent(new BigDecimal("5000.00"));
        lease.setStatus(LeaseStatus.ACTIVE);
        return lease;
    }

    @Test
    void fromExposesTenantAndSpaceBothFlatAndNested() {
        LeaseResponseDTO dto = LeaseResponseDTO.from(lease(tenant(), space()));

        // Flat fields (used by invoice/report style consumers).
        assertEquals(7L, dto.getTenantId());
        assertEquals("Abebe Trading", dto.getTenantName());
        assertEquals(3L, dto.getSpaceId());
        assertEquals("G-01", dto.getUnitNumber());

        // Nested objects (used by the leases list, dashboard and tenant dashboard).
        assertEquals(7L, dto.getTenant().getId());
        assertEquals("Abebe Trading", dto.getTenant().getName());
        assertEquals(3L, dto.getSpace().getId());
        assertEquals("G-01", dto.getSpace().getUnitNumber());
        assertEquals("Office", dto.getSpace().getCategory());
    }

    @Test
    void fromLeavesNestedObjectsNullWhenRelationsAreMissing() {
        LeaseResponseDTO dto = LeaseResponseDTO.from(lease(null, null));

        assertNull(dto.getTenantId());
        assertNull(dto.getTenantName());
        assertNull(dto.getSpaceId());
        assertNull(dto.getUnitNumber());
        assertNull(dto.getTenant());
        assertNull(dto.getSpace());
    }

    @Test
    void serializesNestedTenantAndSpaceObjects() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        JsonNode json = mapper.readTree(
                mapper.writeValueAsString(LeaseResponseDTO.from(lease(tenant(), space()))));

        // The UI reads lease.tenant.name and lease.space.unitNumber / lease.space.category.
        assertEquals("Abebe Trading", json.path("tenant").path("name").asText());
        assertEquals(7L, json.path("tenant").path("id").asLong());
        assertEquals("G-01", json.path("space").path("unitNumber").asText());
        assertEquals("Office", json.path("space").path("category").asText());

        // Flat fields stay in the payload for the other consumers.
        assertEquals("Abebe Trading", json.path("tenantName").asText());
        assertEquals("G-01", json.path("unitNumber").asText());
    }

    @Test
    void fromReturnsNullForNullLease() {
        assertNull(LeaseResponseDTO.from(null));
    }
}
