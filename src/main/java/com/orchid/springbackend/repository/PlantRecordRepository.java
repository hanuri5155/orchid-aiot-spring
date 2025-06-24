package com.orchid.springbackend.repository;

import com.orchid.springbackend.domain.PlantRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface PlantRecordRepository extends JpaRepository<PlantRecord, Long> {
    // 특정 날짜의 기록을 찾는 메서드 추가
    Optional<PlantRecord> findByRecordDate(LocalDate recordDate);
}
