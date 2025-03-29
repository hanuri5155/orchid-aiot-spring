package com.orchid.springbackend.repository;

import com.orchid.springbackend.domain.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
}
