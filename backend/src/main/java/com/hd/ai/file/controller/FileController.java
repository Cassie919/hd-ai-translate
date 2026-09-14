package com.hd.ai.file.controller;

import com.hd.ai.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 上传文件并保存到 MinIO，返回 objectKey 和文件名。
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try {
            String objectKey = "upload/" + UUID.randomUUID() + "/" + file.getOriginalFilename();
            fileService.upload(file, objectKey);
            return ResponseEntity.ok(Map.of(
                    "objectKey", objectKey,
                    "fileName", file.getOriginalFilename()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
