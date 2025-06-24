package com.orchid.springbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SensorDataDto {
    private String deviceId;
    private double temperature;
    private double humidity;
    private double soilTemperature;
    private double soilMoisture;
    private double soilEC;
    private double soilPH;
    private LocalDateTime recordedAt;
    private LocalDateTime lastWatered;
    private LocalDateTime lastLedOn;
    private boolean alertSoilDry;
    private boolean alertLightLow;
}