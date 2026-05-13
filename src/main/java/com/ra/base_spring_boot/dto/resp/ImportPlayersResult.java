package com.ra.base_spring_boot.dto.resp;

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
