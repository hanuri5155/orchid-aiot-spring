package com.orchid.springbackend.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import com.orchid.springbackend.dto.SensorDataDto;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {
    @MessageMapping("/hello") // 클라이언트가 /app/hello 로 메시지 보낼 때
    @SendTo("/topic/sensor")  // 모든 구독자에게 브로드캐스트
    public SensorDataDto sendSensor(SensorDataDto dto) {
        System.out.println("WebSocket 수신됨: " + dto);
        return dto; // 그대로 브로드캐스트
    }
}
