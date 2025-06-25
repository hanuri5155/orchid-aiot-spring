package com.orchid.springbackend.config;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // Spring 설정 클래스임을 명시
public class MqttConfig {

    @Value("${mqtt.broker.url}") // application.properties에서 값 주입
    private String brokerUrl;
    @Value("${mqtt.client.id}") // application.properties에서 값 주입
    private String clientId;
    // @Value("${mqtt.username}") private String mqttUsername;
    // @Value("${mqtt.password}") private String mqttPassword;

    @Bean // Spring 컨테이너에 IMqttClient 타입의 객체(Bean)로 등록
    public IMqttClient mqttClient() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true); // 연결 끊어질 경우 자동 재연결 시도
        options.setCleanSession(true); // 클라이언트 연결 끊김 시 세션 정보 삭제 (이전 메시지 재전송 안 함)
        // if (mqttUsername != null && mqttPassword != null) { // MQTT 브로커에 사용자 인증 설정 시 활성화
        //     options.setUserName(mqttUsername);
        //     options.setPassword(mqttPassword.toCharArray());
        // }

        IMqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        client.connect(options); // MQTT 브로커에 연결 시도
        System.out.println("DEBUG: MQTT Client connected to broker: " + brokerUrl); // 연결 성공 로그

        return client; // 생성된 MQTT 클라이언트 반환
    }
}