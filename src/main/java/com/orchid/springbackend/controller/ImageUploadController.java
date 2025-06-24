package com.orchid.springbackend.controller;

import com.orchid.springbackend.domain.ImageData;
import com.orchid.springbackend.repository.ImageDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/image")
public class ImageUploadController {

    private static final String UPLOAD_DIR = "/home/ubuntu/uploads"; // 실제 서버 경로

    @Autowired
    private ImageDataRepository imageDataRepository;

    @PostMapping
    public ResponseEntity<String> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 고유 파일명 생성
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path savePath = Paths.get(UPLOAD_DIR, filename);

            // 폴더 없으면 생성
            Files.createDirectories(savePath.getParent());

            // 파일 저장
            Files.copy(file.getInputStream(), savePath, StandardCopyOption.REPLACE_EXISTING);

            // DB 저장
            ImageData imageData = new ImageData();
            imageData.setFilename(filename);
            imageData.setFilepath(savePath.toString());
            imageData.setUploadedAt(LocalDateTime.now());

            imageDataRepository.save(imageData);

            return ResponseEntity.ok("업로드 성공: " + filename);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("업로드 실패: " + e.getMessage());
        }
    }
}