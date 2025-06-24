package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.SensorData;
import com.orchid.springbackend.dto.SensorDataDto;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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


    @GetMapping("/latest") // GET 요청으로 /api/sensor/latest 경로에 매핑됩니다.
    public ResponseEntity<SensorDataDto> getLatestSensorData() {
        Optional<SensorData> latestDataOptional = repository.findTopByOrderByRecordedAtDesc();

        if (latestDataOptional.isPresent()) {
            SensorData latestData = latestDataOptional.get();

            // SensorData 엔티티를 클라이언트에게 보낼 SensorDataDto로 변환
            SensorDataDto dto = new SensorDataDto();
            dto.setDeviceId(latestData.getDeviceId());
            dto.setTemperature(latestData.getTemperature());
            dto.setHumidity(latestData.getHumidity());
            dto.setSoilTemperature(latestData.getSoilTemperature());
            dto.setSoilMoisture(latestData.getSoilMoisture());
            dto.setSoilEC(latestData.getSoilEC());
            dto.setSoilPH(latestData.getSoilPH());
            dto.setLastWatered(latestData.getLastWatered());
            dto.setLastLedOn(latestData.getLastLedOn());
            dto.setAlertSoilDry(latestData.isAlertSoilDry()); // boolean getter는 is필드명
            dto.setAlertLightLow(latestData.isAlertLightLow());
            dto.setRecordedAt(latestData.getRecordedAt());

            return ResponseEntity.ok(dto); // 200 OK 상태 코드와 함께 변환된 DTO 응답
        } else {
            return ResponseEntity.noContent().build(); // 데이터가 없을 경우 204 No Content 응답
        }

    }

    // 수동 물 공급 명령 API (POST /api/sensor/control/water)
    @PostMapping("/control/water")
    public ResponseEntity<String> controlWater(@RequestParam String deviceId) {
        System.out.println("DEBUG: Received water control command for device: " + deviceId);
        // ⭐ 여기에 실제 라즈베리파이 또는 장치 제어 로직을 구현합니다. ⭐
        // 예: MQTT 메시지 발행, 다른 IoT 플랫폼 API 호출 등 (이 예시에서는 단순히 로그를 출력)

        // Optional: DB에 마지막 물 공급 시간 업데이트 (가장 최근 센서 데이터를 찾아 업데이트)
        repository.findTopByOrderByRecordedAtDesc().ifPresent(sd -> {
            sd.setLastWatered(LocalDateTime.now());
            repository.save(sd); // 변경 사항을 DB에 저장
            // WebSocket으로 이 업데이트된 센서 데이터를 브로드캐스트하여 앱에 실시간 반영
            messagingTemplate.convertAndSend("/topic/sensor", convertToDto(sd));
        });

        return ResponseEntity.ok("물 공급 명령 전송 완료.");
    }

    // 수동 LED 제어 명령 API (POST /api/sensor/control/led)
    // LED 상태 (켜기/끄기)를 boolean 값으로 받음
    @PostMapping("/control/led")
    public ResponseEntity<String> controlLed(@RequestParam String deviceId, @RequestParam boolean state) {
        System.out.println("DEBUG: Received LED control command for device: " + deviceId + ", state: " + state);
        // 여기에 실제 라즈베리파이 또는 장치 제어 로직을 구현해야함
        // 예: MQTT 메시지 발행, 다른 IoT 플랫폼 API 호출 등

        // Optional: DB에 마지막 LED 켜진 시간 업데이트 (켜질 때만) 및 WebSocket 브로드캐스트
        if (state) { // LED가 켜질 때만 기록
            repository.findTopByOrderByRecordedAtDesc().ifPresent(sd -> {
                sd.setLastLedOn(LocalDateTime.now());
                repository.save(sd); // 변경 사항을 DB에 저장
                messagingTemplate.convertAndSend("/topic/sensor", convertToDto(sd));
            });
        }

        return ResponseEntity.ok("LED 명령 전송 완료. 상태: " + (state ? "켜짐" : "꺼짐"));
    }

    private SensorDataDto convertToDto(SensorData entity) {
        SensorDataDto dto = new SensorDataDto();
        dto.setDeviceId(entity.getDeviceId());
        dto.setTemperature(entity.getTemperature());
        dto.setHumidity(entity.getHumidity());
        dto.setSoilMoisture(entity.getSoilMoisture());
        dto.setRecordedAt(entity.getRecordedAt());
        dto.setLastWatered(entity.getLastWatered());
        dto.setLastLedOn(entity.getLastLedOn());
        dto.setAlertSoilDry(entity.isAlertSoilDry());
        dto.setAlertLightLow(entity.isAlertLightLow());
        return dto;
    }
}
