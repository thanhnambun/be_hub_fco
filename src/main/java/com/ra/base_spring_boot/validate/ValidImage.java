package com.ra.base_spring_boot.validate;

import org.springframework.web.multipart.MultipartFile;

public interface ValidImage {
    String validAndUploadImage(MultipartFile file, long maxSize, String allowedTypes);
}
