package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.SensorData;
import com.orchid.springbackend.dto.SensorDataDto;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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

    // --- 새로 추가되는 부분: 최신 센서 데이터 조회 API ---
    @GetMapping("/latest") // GET 요청으로 /api/sensor/latest 경로에 매핑됩니다.
    public ResponseEntity<SensorDataDto> getLatestSensorData() {
        Optional<SensorData> latestDataOptional = repository.findTopByOrderByRecordedAtDesc();

        if (latestDataOptional.isPresent()) {
            SensorData latestData = latestDataOptional.get();

            // SensorData 엔티티를 클라이언트에게 보낼 SensorDataDto로 변환합니다.
            SensorDataDto dto = new SensorDataDto();
            dto.setDeviceId(latestData.getDeviceId());
            dto.setTemperature(latestData.getTemperature());
            dto.setHumidity(latestData.getHumidity());
            dto.setSoilTemperature(latestData.getSoilTemperature());
            dto.setSoilMoisture(latestData.getSoilMoisture());
            dto.setSoilEC(latestData.getSoilEC());
            dto.setSoilPH(latestData.getSoilPH());

            // 만약 SensorDataDto에 recordedAt 필드를 추가했다면 아래도 포함
            // dto.setRecordedAt(latestData.getRecordedAt());

            return ResponseEntity.ok(dto); // 200 OK 상태 코드와 함께 변환된 DTO 응답
        } else {
            return ResponseEntity.noContent().build(); // 데이터가 없을 경우 204 No Content 응답
        }

    }

}
