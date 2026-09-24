package com.rms.dto;

import com.rms.entity.SpaceStatus;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class SpaceCreateRequest {
    @NotBlank(message = "Unit number is required")
    @Size(min = 2, max = 20, message = "Unit number must be between 2 and 20 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "Unit number can only contain uppercase letters, numbers, and hyphens")
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

    private SpaceStatus status = SpaceStatus.VACANT;
}
