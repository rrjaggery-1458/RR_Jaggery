package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.service.ImageStorageService;
import com.rrjaggery.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/commerce/admin/products")
public class ImageUploadController {

    private final ImageStorageService imageStorageService;

    public ImageUploadController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(@RequestParam("file") MultipartFile file) {
        String imageUrl = imageStorageService.storeImage(file);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("imageUrl", imageUrl), "Image uploaded successfully."));
    }
}
