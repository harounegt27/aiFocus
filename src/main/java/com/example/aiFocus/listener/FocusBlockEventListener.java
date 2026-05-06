package com.example.aiFocus.listener;

import com.example.aiFocus.event.FocusBlockScheduledEvent;
import com.example.aiFocus.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener component for focus block scheduled events.
 * Listens to FocusBlockScheduledEvent and sends confirmation emails.
 * When a focus block is first scheduled, sends an immediate confirmation
 * email to the user with details about the block and reminder timing.
 */
@Component
@Slf4j
public class FocusBlockEventListener {

    private final EmailService emailService;

    /**
     * Constructs FocusBlockEventListener with required dependencies.
     *
     * @param emailService the service for sending confirmation emails
     */
    public FocusBlockEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Listens to FocusBlockScheduledEvent and sends a confirmation email.
     * Called when a focus block is first scheduled in the system.
     *
     * @param event the focus block scheduled event
     */
    @EventListener
    public void onFocusBlockScheduled(FocusBlockScheduledEvent event) {
        try {
            emailService.sendFocusBlockConfirmation(event.getFocusBlock().getUser(), event.getFocusBlock());
            log.info("Focus block confirmation email sent for block: {}", event.getFocusBlock().getId());
        } catch (Exception e) {
            log.error("Error sending confirmation email for block {}: {}", 
                event.getFocusBlock().getId(), e.getMessage(), e);
        }
    }
}

