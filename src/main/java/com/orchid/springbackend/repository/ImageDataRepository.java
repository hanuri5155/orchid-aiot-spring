package com.orchid.springbackend.repository;

import com.orchid.springbackend.domain.ImageData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageDataRepository extends JpaRepository<ImageData, Long> {
}
