package com.orchid.springbackend.dto;

import lombok.Getter;
import lombok.Setter;

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
}