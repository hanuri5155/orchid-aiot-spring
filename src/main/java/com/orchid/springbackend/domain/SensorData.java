package com.orchid.springbackend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;

    private double temperature;
    private double humidity;
    private double soilTemperature;
    private double soilMoisture;
    private double soilEC;
    private double soilPH;

    private LocalDateTime recordedAt;

    private LocalDateTime lastWatered; // 마지막 물 공급 시간
    private LocalDateTime lastLedOn;   // 마지막 LED 켜진 시간
    private boolean alertSoilDry;    // 토양 건조 경고
    private boolean alertLightLow;   // 빛 부족 경고

    private String diseaseName;     // 질병명
    private String diseaseImageUrl; // 질병 이미지 URL

    @PrePersist
    protected void onCreate() {
        this.recordedAt = LocalDateTime.now();
    }
}