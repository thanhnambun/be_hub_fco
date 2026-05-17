package com.fco.platform.player.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImportPlayersRequest {
    @NotBlank(message = "filePath is required")
    private String filePath;
}
