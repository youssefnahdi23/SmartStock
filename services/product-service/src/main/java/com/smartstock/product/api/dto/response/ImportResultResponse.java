package com.smartstock.product.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportResultResponse {

    private String importId;
    private String fileName;
    private int rowsProcessed;
    private int rowsSuccessful;
    private int rowsFailed;
    private List<ImportError> errors;
    private boolean dryRun;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ImportError {
        private int row;
        private String message;
    }
}
