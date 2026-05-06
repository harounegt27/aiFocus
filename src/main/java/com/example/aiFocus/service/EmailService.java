package com.example.aiFocus.service;

import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.ProductivityReport;
import com.example.aiFocus.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * Service class for handling email notifications.
 * Sends reminder emails to users about their focus blocks.
 * Handles mail exceptions gracefully to prevent scheduler crashes.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    /**
     * Constructs EmailService with required dependencies.
     *
     * @param mailSender the JavaMailSender bean for sending emails
     * @param fromEmail the email address to send from (from properties)
     */
    public EmailService(JavaMailSender mailSender, @Value("${spring.mail.username}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    /**
     * Sends a focus block reminder email to the specified user.
     * Includes the block's start time, end time, and a motivational message.
     * Does not rethrow MailException to prevent scheduler crashes.
     *
     * @param user the user to send the reminder to
     * @param block the focus block details for the reminder
     */
    public void sendFocusReminder(User user, FocusBlock block) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom(fromEmail);
            message.setSubject("Focus Block Starting Soon — " + block.getStartTime().toLocalDate());

            String body = String.format(
                "Hello %s,\n\n" +
                "Your focus block is starting soon!\n\n" +
                "Start Time: %s\n" +
                "End Time: %s\n\n" +
                "Stay focused and give your best effort. You've got this! 💪\n\n" +
                "Best regards,\n" +
                "aiFocus Team",
                user.getName(),
                block.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                block.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            message.setText(body);
            mailSender.send(message);

            log.info("Reminder email sent to {} for focus block starting at {}", 
                user.getEmail(), block.getStartTime());

        } catch (MailException e) {
            log.error("Failed to send reminder email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Sends a focus block confirmation email to the specified user.
     * Confirms the block scheduling and informs about the reminder schedule.
     *
     * @param user the user to send the confirmation to
     * @param block the focus block details for the confirmation
     */
    public void sendFocusBlockConfirmation(User user, FocusBlock block) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom(fromEmail);
            message.setSubject("Focus Block Confirmed");

            String body = String.format(
                "Hello %s,\n\n" +
                "Your focus block has been confirmed!\n\n" +
                "Block Details:\n" +
                "Start Time: %s\n" +
                "End Time: %s\n\n" +
                "You will receive a reminder 15 minutes before your focus block starts.\n" +
                "Make sure to minimize distractions and stay focused during this time.\n\n" +
                "Best regards,\n" +
                "aiFocus Team",
                user.getName(),
                block.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                block.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );

            message.setText(body);
            mailSender.send(message);

            log.info("Confirmation email sent to {} for focus block at {}", 
                user.getEmail(), block.getStartTime());

        } catch (MailException e) {
            log.error("Failed to send confirmation email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * Sends a weekly productivity report email to the specified user.
     * Includes summary of completed and skipped blocks, total focus time, peak hour, and AI summary.
     * Does not rethrow MailException to prevent scheduler crashes.
     *
     * @param user the user to send the report to
     * @param report the productivity report details
     */
    public void sendWeeklyReport(User user, ProductivityReport report) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom(fromEmail);
            message.setSubject("Your Weekly Focus Report — week of " + report.getWeekStart());

            String peakHourStr = report.getPeakHour() != null ? report.getPeakHour() + "h00" : "N/A";

            String body = String.format(
                "Hi %s,\n\n" +
                "Here is your productivity report for the week of %s:\n\n" +
                "✅ Blocks completed : %d\n" +
                "❌ Blocks skipped   : %d\n" +
                "⏱ Total focus time : %d minutes\n" +
                "🕐 Peak focus hour  : %s\n\n" +
                "AI Summary:\n" +
                "%s\n\n" +
                "Keep up the great work!\n\n" +
                "Best regards,\n" +
                "aiFocus Team",
                user.getName(),
                report.getWeekStart(),
                report.getBlocksCompleted(),
                report.getBlocksSkipped(),
                report.getTotalFocusMinutes(),
                peakHourStr,
                report.getAiSummary()
            );

            message.setText(body);
            mailSender.send(message);

            log.info("Weekly report sent to {}", user.getEmail());

        } catch (MailException e) {
            log.error("Failed to send weekly report to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }
}
