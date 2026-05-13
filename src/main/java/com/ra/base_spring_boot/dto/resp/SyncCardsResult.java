package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncCardsResult {
    private int totalReceived;
    private int totalSuccessful;
    private int totalFailed;
    private long processingTimeMs;
}
