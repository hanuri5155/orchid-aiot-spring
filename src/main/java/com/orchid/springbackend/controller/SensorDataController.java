package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.SensorData;
import com.orchid.springbackend.dto.SensorDataDto;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate; // WebSocket 메시지 전송을 위해 필요
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.IMqttClient; // ✅ MQTT 클라이언트 인터페이스 임포트
import org.eclipse.paho.client.mqttv3.MqttMessage; // ✅ MQTT 메시지 클래스 임포트
import org.eclipse.paho.client.mqttv3.MqttException; // ✅ MQTT 예외 클래스 임포트


@RestController
@RequestMapping("/api/sensor") // 모든 센서 관련 API는 이 경로로 시작
public class SensorDataController {

    private final SensorDataRepository repository;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket 메시지 전송을 위해 필요
    private final IMqttClient mqttClient; // ✅ IMqttClient 주입받도록 선언

    // 생성자: SensorDataRepository, SimpMessagingTemplate, IMqttClient를 주입받습니다.
    @Autowired // @Autowired는 생성자 주입을 명시적으로 표시 (생략 가능하나 명시 권장)
    public SensorDataController(SensorDataRepository repository, SimpMessagingTemplate messagingTemplate, IMqttClient mqttClient) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.mqttClient = mqttClient; // IMqttClient 주입
    }

    // --- 1. 센서 데이터 저장 (POST /api/sensor) ---
    // 주로 IoT 기기에서 센서값을 보낼 때 사용될 API
    @PostMapping
    public String saveSensorData(@RequestBody SensorDataDto dto) {
        // DTO에서 엔티티로 변환
        SensorData data = convertToEntity(dto);
        // recordedAt은 @PrePersist에서 자동 설정되므로 여기서 명시적으로 다시 설정하지 않음.
        // 만약 DTO에 recordedAt이 포함되어 있다면, convertToEntity에서 처리됨.

        repository.save(data);

        // 센서 데이터 저장 후 WebSocket으로 브로드캐스트 (실시간 업데이트를 위해)
        messagingTemplate.convertAndSend("/topic/sensor", convertToDto(data)); // 저장된 최신 센서 데이터를 앱에 푸시

        return "저장 완료!";
    }

    // --- 2. 최신 센서 데이터 조회 (GET /api/sensor/latest) ---
    // '내 식물 정보' 화면에서 초기 데이터 로드 및 새로고침 시 사용
    @GetMapping("/latest")
    public ResponseEntity<SensorDataDto> getLatestSensorData() {
        Optional<SensorData> latestDataOptional = repository.findTopByOrderByRecordedAtDesc();

        if (latestDataOptional.isPresent()) {
            return ResponseEntity.ok(convertToDto(latestDataOptional.get()));
        } else {
            return ResponseEntity.noContent().build(); // 데이터 없으면 204 No Content
        }
    }

    // --- 6. 수동 물 공급 명령 API (POST /api/sensor/control/water) ---
    @PostMapping("/control/water")
    public ResponseEntity<String> controlWater(@RequestParam String deviceId) {
        System.out.println("DEBUG: Received water control command for device: " + deviceId);
        String commandPayload = "w.1"; // 라즈베리파이가 받을 명령 형식 (예: "w.1"은 물 펌프 켜기)

        try {
            if (mqttClient != null && mqttClient.isConnected()) { // MQTT 클라이언트가 연결되어 있는지 확인
                mqttClient.publish("commands/" + deviceId + "/control", new MqttMessage(commandPayload.getBytes())); // MQTT 발행
                System.out.println("DEBUG: MQTT: Water command published to topic commands/" + deviceId + "/control with payload: " + commandPayload);
            } else {
                System.err.println("ERROR: MQTT Client not connected in Spring Boot. Water command failed.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("MQTT 클라이언트 연결 안 됨.");
            }

            repository.findTopByOrderByRecordedAtDesc().ifPresent(sd -> {
                sd.setLastWatered(LocalDateTime.now());
                repository.save(sd);
                messagingTemplate.convertAndSend("/topic/sensor", convertToDto(sd));
            });

            return ResponseEntity.ok("물 공급 명령 전송 완료.");
        } catch (MqttException e) { // MQTT 발행 중 발생할 수 있는 예외 처리
            System.err.println("ERROR: Error publishing MQTT water command: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("물 공급 명령 실패: " + e.getMessage());
        } catch (Exception e) { // 다른 일반적인 예외 처리
            System.err.println("ERROR: General exception during water control: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("물 공급 명령 실패 (내부 오류): " + e.getMessage());
        }
    }

    // --- 7. 수동 LED 제어 명령 API (POST /api/sensor/control/led) ---
    @PostMapping("/control/led")
    public ResponseEntity<String> controlLed(@RequestParam String deviceId, @RequestParam boolean state) {
        System.out.println("DEBUG: Received LED control command for device: " + deviceId + ", state: " + state);
        String commandPayload = "R." + (state ? "1" : "0"); // 라즈베리파이가 받을 명령 형식 (예: "R.1"은 릴레이 켜기)

        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.publish("commands/" + deviceId + "/control", new MqttMessage(commandPayload.getBytes())); // MQTT 발행
                System.out.println("DEBUG: MQTT: LED command published to topic commands/" + deviceId + "/control with payload: " + commandPayload);
            } else {
                System.err.println("ERROR: MQTT Client not connected in Spring Boot. LED command failed.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("MQTT 클라이언트 연결 안 됨.");
            }

            if (state) { // LED가 켜질 때만 기록 (꺼지는 것은 기록하지 않음)
                repository.findTopByOrderByRecordedAtDesc().ifPresent(sd -> {
                    sd.setLastLedOn(LocalDateTime.now());
                    repository.save(sd);
                    messagingTemplate.convertAndSend("/topic/sensor", convertToDto(sd));
                });
            }

            return ResponseEntity.ok("LED 명령 전송 완료. 상태: " + (state ? "켜짐" : "꺼짐"));
        } catch (MqttException e) {
            System.err.println("ERROR: Error publishing MQTT LED command: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("LED 명령 실패: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("ERROR: General exception during LED control: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("LED 명령 실패 (내부 오류): " + e.getMessage());
        }
    }

    // 헬퍼 메서드: 엔티티 <-> DTO 변환 (SensorDataController 내부에서 사용)
    private SensorDataDto convertToDto(SensorData entity) {
        SensorDataDto dto = new SensorDataDto();
        dto.setDeviceId(entity.getDeviceId());
        dto.setTemperature(entity.getTemperature());
        dto.setHumidity(entity.getHumidity());
        dto.setSoilTemperature(entity.getSoilTemperature());
        dto.setSoilMoisture(entity.getSoilMoisture());
        dto.setSoilEC(entity.getSoilEC());
        dto.setSoilPH(entity.getSoilPH());
        dto.setRecordedAt(entity.getRecordedAt());
        dto.setDiseaseName(entity.getDiseaseName());
        dto.setDiseaseImageUrl(entity.getDiseaseImageUrl());
        dto.setLastWatered(entity.getLastWatered());
        dto.setLastLedOn(entity.getLastLedOn());
        dto.setAlertSoilDry(entity.isAlertSoilDry());
        dto.setAlertLightLow(entity.isAlertLightLow());
        return dto;
    }

    private SensorData convertToEntity(SensorDataDto dto) {
        SensorData entity = new SensorData();
        entity.setDeviceId(dto.getDeviceId());
        entity.setTemperature(dto.getTemperature());
        entity.setHumidity(dto.getHumidity());
        entity.setSoilTemperature(dto.getSoilTemperature());
        entity.setSoilMoisture(dto.getSoilMoisture());
        entity.setSoilEC(dto.getSoilEC());
        entity.setSoilPH(dto.getSoilPH());
        entity.setRecordedAt(dto.getRecordedAt() != null ? dto.getRecordedAt() : LocalDateTime.now());
        entity.setDiseaseName(dto.getDiseaseName());
        entity.setDiseaseImageUrl(dto.getDiseaseImageUrl());
        entity.setLastWatered(dto.getLastWatered());
        entity.setLastLedOn(dto.getLastLedOn());
        entity.setAlertSoilDry(dto.isAlertSoilDry());
        entity.setAlertLightLow(dto.isAlertLightLow());
        return entity;
    }
}