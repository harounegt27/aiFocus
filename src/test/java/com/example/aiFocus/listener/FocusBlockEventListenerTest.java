package com.example.aiFocus.listener;

import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.event.FocusBlockScheduledEvent;
import com.example.aiFocus.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

/**
 * Unit tests for FocusBlockEventListener class.
 * Tests the event listening and email confirmation logic.
 */
@ExtendWith(MockitoExtension.class)
class FocusBlockEventListenerTest {

    @Mock
    private EmailService emailService;

    private FocusBlockEventListener focusBlockEventListener;

    @BeforeEach
    void setUp() {
        focusBlockEventListener = new FocusBlockEventListener(emailService);
    }

    /**
     * Test that a confirmation email is sent when a FocusBlockScheduledEvent is published.
     */
    @Test
    void onEvent_sendsConfirmationEmail() {
        // Arrange
        User user = User.builder()
            .id(1L)
            .name("Jane Smith")
            .email("jane@example.com")
            .build();

        LocalDateTime now = LocalDateTime.now();
        FocusBlock block = FocusBlock.builder()
            .id(1L)
            .user(user)
            .startTime(now.plusMinutes(30))
            .endTime(now.plusMinutes(60))
            .status(BlockStatus.SCHEDULED)
            .build();

        FocusBlockScheduledEvent event = new FocusBlockScheduledEvent(block);

        // Act
        focusBlockEventListener.onFocusBlockScheduled(event);

        // Assert
        verify(emailService, times(1)).sendFocusBlockConfirmation(user, block);
    }
}

