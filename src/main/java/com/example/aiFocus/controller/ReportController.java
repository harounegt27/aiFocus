package com.example.aiFocus.controller;

import com.example.aiFocus.dto.PeakHourStat;
import com.example.aiFocus.dto.ReportResponse;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing productivity reports.
 * Provides endpoints for retrieving weekly reports, peak hours statistics, and report history.
 * All endpoints require user authentication.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Retrieves the weekly productivity report for the current week.
     * Returns 404 if no report has been generated yet for the current week.
     *
     * @return the weekly report response
     */
    @GetMapping("/weekly")
    public ResponseEntity<ReportResponse> getWeeklyReport() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ReportResponse report = reportService.getWeeklyReport(user);
        return ResponseEntity.ok(report);
    }

    /**
     * Retrieves the peak focus hours statistics for the authenticated user.
     *
     * @return list of peak hour statistics
     */
    @GetMapping("/peak-hours")
    public ResponseEntity<List<PeakHourStat>> getPeakHours() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<PeakHourStat> peakHours = reportService.getPeakHours(user);
        return ResponseEntity.ok(peakHours);
    }

    /**
     * Retrieves the history of the last 4 weekly reports for the authenticated user.
     *
     * @return list of report responses
     */
    @GetMapping("/history")
    public ResponseEntity<List<ReportResponse>> getReportHistory() {
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ReportResponse> history = reportService.getReportHistory(user);
        return ResponseEntity.ok(history);
    }
}
