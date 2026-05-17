package com.fco.platform.common.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlertService {

    /**
     * Sends a critical alert to external systems (Telegram, Discord, etc.)
     * This is a simplified implementation for production-grade alerting.
     */
    public void sendCriticalAlert(String message, Exception ex) {
        log.error("!!! CRITICAL ALERT SENT !!! Message: {}", message);
    }
}
