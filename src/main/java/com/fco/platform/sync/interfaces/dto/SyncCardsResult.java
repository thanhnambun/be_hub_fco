package com.fco.platform.sync.interfaces.dto;

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
