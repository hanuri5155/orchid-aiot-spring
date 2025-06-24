package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.PlantRecord;
import com.orchid.springbackend.dto.PlantRecordDto;
import com.orchid.springbackend.repository.PlantRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plant_records") // 이 컨트롤러의 기본 경로
public class PlantRecordController {

    private final PlantRecordRepository repository;

    public PlantRecordController(PlantRecordRepository repository) {
        this.repository = repository;
    }

    // --- 1. 모든 식물 기록 조회 (캘린더에 마커 표시) ---
    @GetMapping // GET /api/plant_records 요청 처리
    public ResponseEntity<List<PlantRecordDto>> getAllPlantRecords() {
        List<PlantRecord> records = repository.findAll();
        List<PlantRecordDto> dtos = records.stream().map(this::convertToDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // --- 2. 특정 날짜의 식물 기록 조회 ---
    @GetMapping("/{date}") // GET /api/plant_records/{date} 요청 처리 (예: /api/plant_records/2025-06-24)
    public ResponseEntity<PlantRecordDto> getPlantRecordByDate(@PathVariable("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date) {
        return repository.findByRecordDate(date)
                .map(this::convertToDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build()); // 없으면 404 Not Found
    }

    // --- 3. 식물 기록 추가/수정 (날짜가 이미 있으면 수정, 없으면 추가) ---
    @PostMapping // POST /api/plant_records 요청 처리
    public ResponseEntity<PlantRecordDto> createOrUpdatePlantRecord(@RequestBody PlantRecordDto dto) {
        // 해당 날짜의 기존 기록을 찾습니다.
        Optional<PlantRecord> existingRecordOptional = repository.findByRecordDate(dto.getRecordDate());
        PlantRecord record;

        if (existingRecordOptional.isPresent()) {
            // 기존 기록이 있다면 업데이트
            record = existingRecordOptional.get();
            record.setStatus(dto.getStatus());
            record.setNotes(dto.getNotes());
            record.setDiseaseName(dto.getDiseaseName());
            record.setDiseaseImageUrl(dto.getDiseaseImageUrl());
        } else {
            // 새로운 기록이라면 생성
            record = convertToEntity(dto);
        }

        PlantRecord savedRecord = repository.save(record); // DB에 저장
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedRecord)); // 201 Created 응답
    }

    // DTO를 엔티티로 변환하는 헬퍼 메서드
    private PlantRecord convertToEntity(PlantRecordDto dto) {
        PlantRecord entity = new PlantRecord();
        entity.setId(dto.getId());
        entity.setRecordDate(dto.getRecordDate());
        entity.setStatus(dto.getStatus());
        entity.setNotes(dto.getNotes());
        entity.setDiseaseName(dto.getDiseaseName());
        entity.setDiseaseImageUrl(dto.getDiseaseImageUrl());
        return entity;
    }

    // 엔티티를 DTO로 변환하는 헬퍼 메서드
    private PlantRecordDto convertToDto(PlantRecord entity) {
        PlantRecordDto dto = new PlantRecordDto();
        dto.setId(entity.getId());
        dto.setRecordDate(entity.getRecordDate());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());
        dto.setDiseaseName(entity.getDiseaseName());
        dto.setDiseaseImageUrl(entity.getDiseaseImageUrl());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}