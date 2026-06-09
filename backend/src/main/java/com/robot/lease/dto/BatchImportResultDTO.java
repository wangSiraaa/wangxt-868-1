package com.robot.lease.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchImportResultDTO {
    private int totalCount;
    private int successCount;
    private int failCount;
    private List<String> successOrderNos = new ArrayList<>();
    private List<ImportError> errors = new ArrayList<>();

    @Data
    public static class ImportError {
        private int rowNum;
        private String field;
        private String message;
        private String originalValue;

        public ImportError(int rowNum, String field, String message, String originalValue) {
            this.rowNum = rowNum;
            this.field = field;
            this.message = message;
            this.originalValue = originalValue;
        }
    }
}
