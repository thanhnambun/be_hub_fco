package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportPlayersRequest {
    @NotBlank(message = "filePath is required")
    private String filePath;
}
