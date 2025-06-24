package com.orchid.springbackend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class PlantRecordDto {
    private Long id;
    private LocalDate recordDate;
    private String status;
    private String notes;
    private String diseaseName;
    private String diseaseImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}