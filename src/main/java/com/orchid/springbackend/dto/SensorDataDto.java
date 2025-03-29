package com.orchid.springbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SensorDataDto {
    private String deviceId;
    private float temperature;
    private float humidity;
    private float soilMoisture;
    private float npkN;
    private float npkP;
    private float npkK;
}