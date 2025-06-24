package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.SensorData;
import com.orchid.springbackend.dto.SensorDataDto;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sensor")
public class SensorDataController {
    // 테스트 2
    private final SensorDataRepository repository;

    public SensorDataController(SensorDataRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public String saveSensorData(@RequestBody SensorDataDto dto) {
        SensorData data = new SensorData();
        data.setDeviceId(dto.getDeviceId());
        data.setTemperature(dto.getTemperature());
        data.setHumidity(dto.getHumidity());
        data.setSoilMoisture(dto.getSoilMoisture());
        data.setNpkN(dto.getNpkN());
        data.setNpkP(dto.getNpkP());
        data.setNpkK(dto.getNpkK());

        repository.save(data);
        return "저장 완료!";
    }
}
