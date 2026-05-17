package com.fco.platform.common.validation;

import org.springframework.web.multipart.MultipartFile;

public interface ValidImage {
    String validAndUploadImage(MultipartFile file, long maxSize, String allowedTypes);
}
