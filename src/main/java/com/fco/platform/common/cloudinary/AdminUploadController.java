package com.fco.platform.common.cloudinary;

import com.fco.platform.common.dto.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Endpoint dùng riêng cho Admin để upload ảnh lên Cloudinary.
 * Trả về secure_url để frontend lưu vào trường flagUrl / logoUrl / crestUrl.
 */
@RestController
@RequestMapping("/api/v1/admin/upload")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminUploadController {

    private final CloudinaryService cloudinaryService;

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseWrapper<String>> uploadImage(
            @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ResponseWrapper.<String>builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message("File không được để trống")
                            .build()
            );
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            return ResponseEntity.badRequest().body(
                    ResponseWrapper.<String>builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message("Kích thước file không được vượt quá 5MB")
                            .build()
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return ResponseEntity.badRequest().body(
                    ResponseWrapper.<String>builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .code(HttpStatus.BAD_REQUEST.value())
                            .message("Chỉ hỗ trợ file ảnh (image/*)")
                            .build()
            );
        }

        try {
            Map result = cloudinaryService.uploadFile(file);
            String secureUrl = result.get("secure_url").toString();
            return ResponseEntity.ok(
                    ResponseWrapper.<String>builder()
                            .status(HttpStatus.OK)
                            .code(HttpStatus.OK.value())
                            .data(secureUrl)
                            .build()
            );
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ResponseWrapper.<String>builder()
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Upload ảnh thất bại: " + e.getMessage())
                            .build()
            );
        }
    }
}
