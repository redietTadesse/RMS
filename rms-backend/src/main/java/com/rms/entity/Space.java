package com.rms.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Entity
@Table(name = "spaces", uniqueConstraints = @UniqueConstraint(columnNames = "unit_number"))
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Unit number is required")
    @Column(name = "unit_number", nullable = false)
    private String unitNumber;

    @NotNull(message = "Floor is required")
    private Integer floor;

    @NotBlank(message = "Category is required")
    private String category; 

    @NotNull(message = "Size is required")
    @Positive(message = "Size must be positive")
    private BigDecimal size; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceStatus status = SpaceStatus.VACANT;

    
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
}
