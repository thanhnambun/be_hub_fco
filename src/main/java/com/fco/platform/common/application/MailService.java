package com.fco.platform.common.application;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendResetPasswordEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[FCO HUB] YEU CAU DAT LAI MAT KHAU");
        message.setText(
                "Xin chao,\n\n"
                        + "Chung toi da nhan duoc yeu cau dat lai mat khau cho tai khoan cua ban.\n"
                        + "Vui long truy cap link duoi day trong 15 phut de tao mat khau moi:\n\n"
                        + resetLink
                        + "\n\nNeu ban khong thuc hien yeu cau nay, vui long bo qua email nay.\n\n"
                        + "FCO HUB Team"
        );
        mailSender.send(message);
    }
}
