package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.SensorData;
import com.orchid.springbackend.dto.SensorDataDto;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensor")
public class SensorDataController {

    private final SensorDataRepository repository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 전송용

    @Autowired
    public SensorDataController(SensorDataRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public ResponseEntity<Void> saveSensorData(@RequestBody SensorDataDto dto) {
        SensorData data = new SensorData();
        data.setDeviceId(dto.getDeviceId());
        data.setTemperature(dto.getTemperature());
        data.setHumidity(dto.getHumidity());
        data.setSoilTemperature(dto.getSoilTemperature());
        data.setSoilMoisture(dto.getSoilMoisture());
        data.setSoilEC(dto.getSoilEC());
        data.setSoilPH(dto.getSoilPH());

        repository.save(data); // @PrePersist로 recordedAt 자동 설정됨

        messagingTemplate.convertAndSend("/topic/sensor", dto); // WebSocket 푸시

        return ResponseEntity.ok().build(); // 200 OK 응답
    }
}
