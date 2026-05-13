package com.ra.base_spring_boot.validate.impl;

import com.ra.base_spring_boot.cloudinary.CloudinaryService;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.validate.ValidImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ValidImageImpl implements ValidImage {
    private final CloudinaryService cloudinaryService;

    @Override
    public String validAndUploadImage(MultipartFile file, long maxSize, String allowedTypes) {
        if (file == null || file.isEmpty())
            throw new HttpBadRequest("File cannot be empty!");

        if (file.getSize() > maxSize)
            throw new HttpBadRequest("File size must not exceed " + (maxSize / (1024 * 1024)) + "MB!");

        if (allowedTypes == null || allowedTypes.isBlank())
            throw new IllegalArgumentException("Allowed content types must be provided!");

        Set<String> allowed = Arrays.stream(allowedTypes.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        String ct = file.getContentType();
        if (ct == null || !allowed.contains(ct.toLowerCase()))
            throw new HttpBadRequest("Invalid file type! Allowed: " + String.join(", ", allowed));

        try {
            java.util.Map result = cloudinaryService.uploadFile(file);
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }
}
