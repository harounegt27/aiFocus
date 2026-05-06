package com.example.aiFocus.scheduler;

import com.example.aiFocus.entity.ProductivityReport;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.repository.UserRepository;
import com.example.aiFocus.service.EmailService;
import com.example.aiFocus.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler for generating and sending weekly productivity reports.
 * Runs every Monday at 8:00 AM to send reports to all users.
 */
@Component
@Slf4j
public class WeeklyReportScheduler {

    private final ReportService reportService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public WeeklyReportScheduler(ReportService reportService,
                                 UserRepository userRepository,
                                 EmailService emailService) {
        this.reportService = reportService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Generates and sends weekly productivity reports to all users.
     * Scheduled to run every Monday at 8:00 AM.
     * Continues processing other users even if one fails.
     */
    // 0 */2 * * * * - every 2 minutes for testing, change to "0 0 8 * * MON" for production
    @Scheduled(cron = "0 0 8 * * MON")
    public void generateAndSendWeeklyReports() {
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        List<User> users = userRepository.findAll();

        for (User user : users) {
            try {
                ProductivityReport report = reportService.generateWeeklyReport(user, weekStart);
                emailService.sendWeeklyReport(user, report);
                log.info("Weekly report sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to generate report for {}: {}", user.getEmail(), e.getMessage(), e);
            }
        }
    }
}
