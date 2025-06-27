package com.orchid.springbackend.controller;
import com.orchid.springbackend.domain.ImageData;
import com.orchid.springbackend.domain.SensorData;
import com.orchid.springbackend.dto.SensorDataDto;
import com.orchid.springbackend.repository.ImageDataRepository;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate; // WebSocket 메시지 전송을 위해 추가
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.MultipartBodyBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/image") // 이미지 관련 모든 API는 이 경로로 시작
public class ImageController {

    private static final String UPLOAD_DIR = "/home/ubuntu/uploads"; // 이미지가 저장될 서버의 로컬 경로
    private static final String FASTAPI_PREDICT_URL = "http://113.198.233.218:8000/predict"; // FastAPI 서버 URL

    private final ImageDataRepository imageDataRepository;
    private final SensorDataRepository sensorDataRepository;
    private final SimpMessagingTemplate messagingTemplate; // SimpMessagingTemplate 주입

    // 웹에서 이미지를 직접 서빙할 때 사용할 기본 URL (nginx, Apache 등으로 /uploads 폴더를 웹 서빙하도록 설정해야 함)
    private static final String WEB_ACCESS_BASE_URL = "http://134.185.115.80:8080/uploads/"; // 웹에서 접근 가능한 이미지 URL의 기본 경로

    @Autowired // 생성자 주입 방식으로 WebClient, SimpMessagingTemplate 초기화
    public ImageController(ImageDataRepository imageDataRepository, SensorDataRepository sensorDataRepository,
                           WebClient.Builder webClientBuilder, SimpMessagingTemplate messagingTemplate) {
        this.imageDataRepository = imageDataRepository;
        this.sensorDataRepository = sensorDataRepository;
        //this.webClient = webClientBuilder.baseUrl(FASTAPI_PREDICT_URL).build();
        this.messagingTemplate = messagingTemplate; // SimpMessagingTemplate 주입
    }

    // 이미지 전송 API (POST /api/image/upload) ---
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID().toString() + "_" + originalFilename;
        Path savePath = Paths.get(UPLOAD_DIR, uniqueFilename);
        String fileUrl = WEB_ACCESS_BASE_URL + uniqueFilename;

        try {
            Files.createDirectories(savePath.getParent());
            Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

            ImageData imageData = new ImageData();
            imageData.setFilename(uniqueFilename);
            imageData.setFilepath(savePath.toString());
            imageData.setFileUrl(fileUrl);
            imageData.setUploadedAt(LocalDateTime.now());
            imageDataRepository.save(imageData);

            System.out.println("DEBUG: Image uploaded and saved: " + uniqueFilename + " at " + fileUrl);
            // 이미지 URL을 WebSocket으로 브로드캐스트 (실시간 업데이트)
            messagingTemplate.convertAndSend("/topic/image", Map.of("imageUrl", fileUrl));

            return ResponseEntity.ok(Map.of(
                    "message", "업로드 성공",
                    "imageUrl", fileUrl
            ));

        } catch (IOException e) {
            System.err.println("ERROR: Image upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "업로드 실패",
                    "error", e.getMessage()
            ));
        }
    }

    // SensorData 엔티티를 SensorDataDto로 변환
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
        dto.setDiseaseName(entity.getDiseaseName());
        dto.setDiseaseImageUrl(entity.getDiseaseImageUrl());
        return dto;
    }
}