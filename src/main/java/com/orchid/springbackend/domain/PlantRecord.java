package com.orchid.springbackend.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlantRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private LocalDate recordDate; // 기록 날짜 (YYYY-MM-DD 형식)

    private String status; // 식물 상태 (예: "정상", "문제 발생")
    @Column(length = 1000)
    private String notes;  // 관리 메모

    private String diseaseName; // 질병이 있다면 병명
    private String diseaseImageUrl; // 질병 관련 이미지 URL

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now(); // 기록 생성 시간
    private LocalDateTime updatedAt = LocalDateTime.now(); // 기록 마지막 업데이트 시간

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(); // 업데이트 시간 자동 갱신
    }
}