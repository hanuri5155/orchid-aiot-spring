package com.orchid.springbackend.controller;
import com.orchid.springbackend.domain.ImageData;
import com.orchid.springbackend.domain.SensorData; // SensorData에 질병 진단 결과 저장용
import com.orchid.springbackend.dto.SensorDataDto; // SensorDataDto 사용을 위해
import com.orchid.springbackend.repository.ImageDataRepository;
import com.orchid.springbackend.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate; // WebSocket 메시지 전송을 위해 추가
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.client.MultipartBodyBuilder; // WebClient로 MultipartFile 전송을 위해

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors; // List<Object>를 List<String>으로 변환 시 필요

@RestController
@RequestMapping("/api/image") // 이미지 관련 모든 API는 이 경로로 시작
public class ImageController {

    private static final String UPLOAD_DIR = "/home/ubuntu/uploads"; // 이미지가 저장될 서버의 로컬 경로
    private static final String FASTAPI_PREDICT_URL = "http://[FASTAPI_서버_IP_주소]:[FASTAPI_포트]/predict"; // FastAPI 서버 URL

    private final ImageDataRepository imageDataRepository;
    private final SensorDataRepository sensorDataRepository;
    //private final WebClient webClient;
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

    // --- 1. 이미지 업로드 API (POST /api/image/upload) ---
    // 하드웨어 (라즈베리파이)가 5분마다 이미지 파일을 보낼 때 사용
    // Flutter 앱에서도 수동으로 이미지 업로드 시 사용 가능 (예: 갤러리에서 선택)
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
            // /topic/image 채널을 새로 만들거나, 기존 /topic/sensor를 활용할 수 있습니다.
            // 여기서는 /topic/image로 새로운 채널을 가정합니다.
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

    // --- 3. 질병 진단 요청 API (POST /api/image/diagnose) ---
    // Flutter 앱에서 '질병 진단' 버튼을 눌렀을 때 사용
    // 이 API는 가장 최근에 업로드된 이미지를 FastAPI로 보내 진단 요청을 수행합니다.
//    @PostMapping("/diagnose")
//    public ResponseEntity<Map<String, Object>> diagnoseImage() {
//        Optional<ImageData> latestImageOptional = imageDataRepository.findTopByOrderByUploadedAtDesc();
//        if (latestImageOptional.isEmpty()) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "진단할 이미지가 없습니다."));
//        }
//
//        ImageData latestImage = latestImageOptional.get();
//        File imageFile = new File(latestImage.getFilepath()); // 로컬에 저장된 파일 로드
//
//        if (!imageFile.exists()) {
//            System.err.println("ERROR: Image file not found for diagnosis: " + latestImage.getFilepath());
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "서버에 이미지가 없습니다."));
//        }
//
//        try {
//            // 이미지를 FastAPI 서버로 전송하여 진단 요청
//            MultipartBodyBuilder builder = new MultipartBodyBuilder();
//            builder.part("file", new ByteArrayResource(Files.readAllBytes(imageFile.toPath())), MediaType.IMAGE_JPEG)
//                    .filename(latestImage.getFilename());
//
//            WebClient.ResponseSpec responseSpec = webClient.post()
//                    .uri("/predict") // FastAPI의 /predict 엔드포인트
//                    .contentType(MediaType.MULTIPART_FORM_DATA)
//                    .body(BodyInserters.fromMultipartData(builder.build()))
//                    .retrieve();
//
//            Map<String, Object> predictionResult = responseSpec.bodyToMono(Map.class).block(); // 진단 결과 대기
//            System.out.println("DEBUG: Prediction from FastAPI: " + predictionResult);
//
//            // 진단 결과 데이터베이스 업데이트 (SensorData에 저장)
//            if (predictionResult != null && predictionResult.containsKey("predictions")) {
//                // ✅ 수정된 부분: List<Object>로 받은 후, 스트림을 사용하여 각 요소를 String으로 안전하게 변환
//                List<Object> rawPredictions = (List<Object>) predictionResult.get("predictions");
//                List<String> predictions = rawPredictions.stream()
//                        .map(Object::toString)
//                        .collect(Collectors.toList());
//
//                String diseaseType = predictions.isEmpty() ? "healthy" : predictions.get(0);
//
//                sensorDataRepository.findTopByOrderByRecordedAtDesc().ifPresent(sd -> {
//                    sd.setDiseaseName(diseaseType.equals("healthy") ? null : diseaseType);
//                    sd.setDiseaseImageUrl(latestImage.getFileUrl()); // 진단된 이미지의 URL 저장
//                    sensorDataRepository.save(sd);
//                    System.out.println("DEBUG: SensorData updated with diagnosis: " + diseaseType);
//
//                    // WebSocket으로 업데이트된 센서 데이터를 브로드캐스트 (실시간 반영)
//                    // SensorDataController의 convertToDto와 유사한 변환이 필요
//                    // 여기서는 임시로 DTO를 다시 만들거나, SensorDataController의 헬퍼 메서드를 재사용해야 합니다.
//                    // 복잡성 때문에 이 예시에서는 SensorDataDto를 여기서 직접 구성하지 않고,
//                    // 실제 구현에서는 공통 서비스로 추출하거나, SensorDataController에서 브로드캐스트를 담당하게 합니다.
//                    // (만약 ImageController에도 SimpMessagingTemplate이 주입되어 있고 convertToDto가 있다면 아래 주석 해제 가능)
//                    // messagingTemplate.convertAndSend("/topic/sensor", convertToDto(sd));
//                });
//            } else {
//                System.err.println("ERROR: FastAPI did not return 'predictions'. Raw response: " + predictionResult);
//                return ResponseEntity.status(HttpStatus.OK).body(Map.of("message", "진단 결과 형식 오류."));
//            }
//
//            return ResponseEntity.ok(predictionResult);
//
//        } catch (IOException e) {
//            System.err.println("ERROR: File read/write or FastAPI communication failed: " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "파일 처리 또는 AI 통신 실패: " + e.getMessage()));
//        } catch (Exception e) {
//            System.err.println("ERROR: FastAPI prediction failed: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "AI 진단 실패: " + e.getMessage()));
//        }
//    }

    // ✅ 헬퍼 메서드: SensorData 엔티티를 SensorDataDto로 변환 (ImageController 내부에서 필요)
    // 이 메서드는 SensorDataController의 convertToDto와 동일하게 구현되어야 합니다.
    private SensorDataDto convertToDto(SensorData entity) {
        SensorDataDto dto = new SensorDataDto();
        dto.setDeviceId(entity.getDeviceId());
        dto.setTemperature(entity.getTemperature());
        dto.setHumidity(entity.getHumidity());
        dto.setSoilMoisture(entity.getSoilMoisture());
        dto.setRecordedAt(entity.getRecordedAt());
        // 센서 데이터 외 추가 필드들
        dto.setLastWatered(entity.getLastWatered());
        dto.setLastLedOn(entity.getLastLedOn());
        dto.setAlertSoilDry(entity.isAlertSoilDry());
        dto.setAlertLightLow(entity.isAlertLightLow());
        dto.setDiseaseName(entity.getDiseaseName());
        dto.setDiseaseImageUrl(entity.getDiseaseImageUrl());
        // NPK, pH, EC, SoilTemperature 등이 추가된다면 여기에 추가
        // 현재 SensorDataDto에는 NPK, soilTemperature, soilEC, soilPH 필드가 없으므로,
        // 필요하다면 SensorDataDto에 추가하고 이 convertToDto 메서드에도 반영해야 합니다.
        // 예를 들어, entity.getSoilTemperature(), entity.getSoilEC(), entity.getSoilPH(), entity.getNpkN(), entity.getNpkP(), entity.getNpkK()
        // 필드를 DTO에도 추가하고 여기서 set 해야 합니다.
        return dto;
    }
}