package com.example.aiFocus.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.aiFocus.entity.ProductivityReport;

public record ReportResponse(
    Long id,
    LocalDate weekStart,
    int totalFocusMinutes,
    int blocksCompleted,
    int blocksSkipped,
    Integer peakHour,
    String aiSummary,
    LocalDateTime generatedAt
) {
    public static ReportResponse from(ProductivityReport report) {
        return new ReportResponse(
            report.getId(),
            report.getWeekStart(),
            report.getTotalFocusMinutes(),
            report.getBlocksCompleted(),
            report.getBlocksSkipped(),
            report.getPeakHour(),
            report.getAiSummary(),
            report.getGeneratedAt()
        );
    }
}
