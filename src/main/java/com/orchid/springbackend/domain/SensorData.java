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

    private float temperature;
    private float humidity;
    private float soilMoisture;
    private float npkN;
    private float npkP;
    private float npkK;

    private LocalDateTime recordedAt = LocalDateTime.now();
}