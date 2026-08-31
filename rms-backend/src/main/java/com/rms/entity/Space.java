package com.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "spaces", uniqueConstraints = @UniqueConstraint(columnNames = "unit_number"))
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Unit number is required")
    @Size(min = 2, max = 20, message = "Unit number must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Unit number can only contain uppercase letters, numbers, and hyphens")
    @Column(name = "unit_number", nullable = false)
    private String unitNumber;

    @NotNull(message = "Floor is required")
    @Min(value = 0, message = "Floor must be 0 or greater")
    @Max(value = 50, message = "Floor cannot exceed 50")
    private Integer floor;

    @NotBlank(message = "Category is required")
    @Size(min = 2, max = 50, message = "Category must be between 2 and 50 characters")
    private String category;

    @NotNull(message = "Size is required")
    @Positive(message = "Size must be a positive number")
    @DecimalMin(value = "0.01", message = "Size must be at least 0.01")
    @DecimalMax(value = "99999.99", message = "Size cannot exceed 99,999.99")
    private BigDecimal size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceStatus status = SpaceStatus.VACANT;

    @Version
    @Column(name = "version")
    private Long version;

    // Getters and Setters (existing)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUnitNumber() { return unitNumber; }
    public void setUnitNumber(String unitNumber) { this.unitNumber = unitNumber; }

    public Integer getFloor() { return floor; }
    public void setFloor(Integer floor) { this.floor = floor; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public BigDecimal getSize() { return size; }
    public void setSize(BigDecimal size) { this.size = size; }

    public SpaceStatus getStatus() { return status; }
    public void setStatus(SpaceStatus status) { this.status = status; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}