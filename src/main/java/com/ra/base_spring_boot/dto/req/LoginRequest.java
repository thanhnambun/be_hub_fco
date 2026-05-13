package com.ra.base_spring_boot.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Vui lòng nhập Username hoặc Email")
    private String identifier;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
