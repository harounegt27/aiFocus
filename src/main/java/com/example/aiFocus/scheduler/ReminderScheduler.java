package com.example.aiFocus.scheduler;

import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.repository.FocusBlockRepository;
import com.example.aiFocus.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler component for sending focus block reminders.
 * Periodically checks for upcoming focus blocks and sends reminder emails.
 * Runs every 5 minutes to identify blocks starting within a 10-20 minute window.
 */
@Component
@Slf4j
public class ReminderScheduler {

    private final FocusBlockRepository focusBlockRepository;
    private final EmailService emailService;

    /**
     * Constructs ReminderScheduler with required dependencies.
     *
     * @param focusBlockRepository the repository for accessing focus blocks
     * @param emailService the service for sending reminder emails
     */
    public ReminderScheduler(FocusBlockRepository focusBlockRepository, EmailService emailService) {
        this.focusBlockRepository = focusBlockRepository;
        this.emailService = emailService;
    }

    /**
     * Checks for upcoming focus blocks and sends reminders.
     * Runs every 5 minutes (300000 ms).
     * Sends reminders for blocks starting between now+10min and now+20min
     * to avoid duplicate reminders on each execution.
     */
    @Scheduled(fixedRate = 300000)
    public void checkAndSendReminders() {
        log.debug("Starting reminder check...");

        try {
            // Get all SCHEDULED focus blocks
            List<FocusBlock> scheduledBlocks = focusBlockRepository.findByStatus(BlockStatus.SCHEDULED);

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime tenMinutesLater = now.plusMinutes(10);
            LocalDateTime twentyMinutesLater = now.plusMinutes(20);

            for (FocusBlock block : scheduledBlocks) {
                LocalDateTime blockStartTime = block.getStartTime();

                // Check if block is in the 10-20 minute window
                if (!blockStartTime.isBefore(tenMinutesLater) && blockStartTime.isBefore(twentyMinutesLater)) {
                    // Send reminder
                    emailService.sendFocusReminder(block.getUser(), block);
                    log.info("Reminder sent to {} for block starting at {}", 
                        block.getUser().getEmail(), blockStartTime);
                }
            }

            log.debug("Reminder check completed successfully");

        } catch (Exception e) {
            log.error("Error during reminder check: {}", e.getMessage(), e);
        }
    }
}

