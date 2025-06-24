package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.SensorData;
import com.orchid.springbackend.dto.SensorDataDto;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

        String topic = "commands/" + deviceId + "/water"; // 제어 토픽 (예: commands/ORCHID_CONTROL_001/water)
        String messagePayload = "{\"action\": \"start_watering\", \"duration\": 10}"; // 10초간 물 공급 (예시)

        try {
            // 실제 MQTT 메시지 발행 로직
            // if (mqttClient != null && mqttClient.isConnected()) {
            //     mqttClient.publish(topic, new MqttMessage(messagePayload.getBytes()));
            //     System.out.println("MQTT: Water command published to topic " + topic);
            // } else {
            //     System.err.println("MQTT client not connected. Water command failed.");
            //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("MQTT client not ready.");
            // }

            // ... (DB 업데이트 및 WebSocket 브로드캐스트 로직은 동일) ...
            repository.findTopByOrderByRecordedAtDesc().ifPresent(sd -> {
                sd.setLastWatered(LocalDateTime.now());
                repository.save(sd);
                messagingTemplate.convertAndSend("/topic/sensor", convertToDto(sd));
            });

            return ResponseEntity.ok("물 공급 명령 전송 완료.");
        } catch (Exception e) {
            System.err.println("Error publishing MQTT water command: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("물 공급 명령 실패: " + e.getMessage());
        }
    }


    // 수동 LED 제어 명령 API (POST /api/sensor/control/led)
    // LED 상태 (켜기/끄기)를 boolean 값으로 받음
    @PostMapping("/control/led")
    public ResponseEntity<String> controlLed(@RequestParam String deviceId, @RequestParam boolean state) {
        System.out.println("DEBUG: Received LED control command for device: " + deviceId + ", state: " + state);

        String topic = "commands/" + deviceId + "/led"; // 제어 토픽 (예: commands/ORCHID_CONTROL_001/led)
        String messagePayload = "{\"action\": \"" + (state ? "turn_on" : "turn_off") + "\"}";

        try {
            // 실제 MQTT 메시지 발행 로직
            // if (mqttClient != null && mqttClient.isConnected()) {
            //     mqttClient.publish(topic, new MqttMessage(messagePayload.getBytes()));
            //     System.out.println("MQTT: LED command published to topic " + topic);
            // } else {
            //     System.err.println("MQTT client not connected. LED command failed.");
            //     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("MQTT client not ready.");
            // }

            // ... (DB 업데이트 및 WebSocket 브로드캐스트 로직은 동일) ...
            if (state) {
                repository.findTopByOrderByRecordedAtDesc().ifPresent(sd -> {
                    sd.setLastLedOn(LocalDateTime.now());
                    repository.save(sd);
                    messagingTemplate.convertAndSend("/topic/sensor", convertToDto(sd));
                });
            }

            return ResponseEntity.ok("LED 명령 전송 완료. 상태: " + (state ? "켜짐" : "꺼짐"));
        } catch (Exception e) {
            System.err.println("Error publishing MQTT LED command: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("LED 명령 실패: " + e.getMessage());
        }
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
