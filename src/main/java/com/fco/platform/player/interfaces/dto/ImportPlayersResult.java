package com.fco.platform.player.interfaces.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ImportPlayersResult {
    private int totalRows;
    private int inserted;
    private int updated;
    private int skipped;
}
