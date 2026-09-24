package com.rms.dto;

import com.rms.entity.Space;
import com.rms.entity.SpaceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpaceResponseDTO {
    private Long id;
    private String unitNumber;
    private Integer floor;
    private String category;
    private BigDecimal size;
    private SpaceStatus status;

    public static SpaceResponseDTO from(Space space) {
        if (space == null) return null;
        return SpaceResponseDTO.builder()
                .id(space.getId())
                .unitNumber(space.getUnitNumber())
                .floor(space.getFloor())
                .category(space.getCategory())
                .size(space.getSize())
                .status(space.getStatus())
                .build();
    }
}
