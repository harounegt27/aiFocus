package com.example.aiFocus.service;

import com.example.aiFocus.dto.PeakHourStat;
import com.example.aiFocus.dto.ReportResponse;
import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.ProductivityReport;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.exception.ResourceNotFoundException;
import com.example.aiFocus.repository.FocusBlockRepository;
import com.example.aiFocus.repository.ProductivityReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for generating and retrieving weekly productivity reports.
 * Handles calculation of focus metrics, AI summary generation, and report persistence.
 */
@Service
@Slf4j
public class ReportService {

    private final ProductivityReportRepository reportRepository;
    private final FocusBlockRepository focusBlockRepository;
    private final AiService aiService;
    private final EmailService emailService;

    public ReportService(ProductivityReportRepository reportRepository,
                         FocusBlockRepository focusBlockRepository,
                         AiService aiService,
                         EmailService emailService) {
        this.reportRepository = reportRepository;
        this.focusBlockRepository = focusBlockRepository;
        this.aiService = aiService;
        this.emailService = emailService;
    }

    /**
     * Generates a weekly productivity report for the given user and week start date.
     * If a report already exists for the week, returns it without regeneration.
     * Calculates metrics from completed and skipped focus blocks, generates AI summary,
     * and persists the report.
     *
     * @param user the user for whom to generate the report
     * @param weekStart the start date of the week (Monday)
     * @return the generated or existing productivity report
     */
    public ProductivityReport generateWeeklyReport(User user, LocalDate weekStart) {
        Optional<ProductivityReport> existing = reportRepository.findByUserAndWeekStart(user, weekStart);
        if (existing.isPresent()) {
            return existing.get();
        }

        LocalDateTime from = weekStart.atStartOfDay();
        LocalDateTime to = weekStart.plusDays(7).atStartOfDay();

        List<FocusBlock> completedBlocks = focusBlockRepository.findByUserAndStatusAndStartTimeBetween(
                user, BlockStatus.COMPLETED, from, to);
        List<FocusBlock> skippedBlocks = focusBlockRepository.findByUserAndStatusAndStartTimeBetween(
                user, BlockStatus.SKIPPED, from, to);

        int totalFocusMinutes = completedBlocks.stream()
                .mapToInt(block -> (int) Duration.between(block.getStartTime(), block.getEndTime()).toMinutes())
                .sum();
        int blocksCompleted = completedBlocks.size();
        int blocksSkipped = skippedBlocks.size();

        Integer peakHour = completedBlocks.stream()
                .collect(Collectors.groupingBy(block -> block.getStartTime().getHour(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String aiSummary = aiService.generateReportSummary(totalFocusMinutes, blocksCompleted, blocksSkipped, peakHour, user.getName());

        ProductivityReport report = ProductivityReport.builder()
                .user(user)
                .weekStart(weekStart)
                .totalFocusMinutes(totalFocusMinutes)
                .blocksCompleted(blocksCompleted)
                .blocksSkipped(blocksSkipped)
                .peakHour(peakHour)
                .aiSummary(aiSummary)
                .build();

        return reportRepository.save(report);
    }

    /**
     * Retrieves the weekly report for the current week.
     * Throws ResourceNotFoundException if no report exists for the current week.
     *
     * @param user the user requesting the report
     * @return the report response for the current week
     * @throws ResourceNotFoundException if no report is found
     */
    public ReportResponse getWeeklyReport(User user) {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        Optional<ProductivityReport> report = reportRepository.findByUserAndWeekStart(user, weekStart);
        if (report.isEmpty()) {
            throw new ResourceNotFoundException("No report found for week of " + weekStart);
        }
        return ReportResponse.from(report.get());
    }

    /**
     * Retrieves the peak focus hours statistics for the user.
     * Groups completed focus blocks by hour and counts occurrences.
     *
     * @param user the user for whom to retrieve peak hours
     * @return list of peak hour statistics sorted by hour
     */
    public List<PeakHourStat> getPeakHours(User user) {
        List<FocusBlock> completedBlocks = focusBlockRepository.findByUserAndStatus(user, BlockStatus.COMPLETED);
        return completedBlocks.stream()
                .collect(Collectors.groupingBy(block -> block.getStartTime().getHour(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new PeakHourStat(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PeakHourStat::hour))
                .collect(Collectors.toList());
    }

    /**
     * Retrieves the history of the last 4 weekly reports for the user.
     *
     * @param user the user requesting the history
     * @return list of report responses for the last 4 weeks
     */
    public List<ReportResponse> getReportHistory(User user) {
        List<ProductivityReport> reports = reportRepository.findTop4ByUserOrderByWeekStartDesc(user);
        return reports.stream()
                .map(ReportResponse::from)
                .collect(Collectors.toList());
    }
}
