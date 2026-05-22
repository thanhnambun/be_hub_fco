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
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[FCO HUB] MA XAC NHAN DAT LAI MAT KHAU");
        message.setText(
                "Xin chao,\n\n"
                        + "Chung toi da nhan duoc yeu cau dat lai mat khau cho tai khoan cua ban.\n\n"
                        + "Ma xac nhan cua ban la:\n\n"
                        + "    " + otp + "\n\n"
                        + "Ma nay co hieu luc trong 15 phut. Vui long khong chia se ma nay voi bat ky ai.\n\n"
                        + "Neu ban khong thuc hien yeu cau nay, vui long bo qua email nay.\n\n"
                        + "FCO HUB Team"
        );
        mailSender.send(message);
    }
}
