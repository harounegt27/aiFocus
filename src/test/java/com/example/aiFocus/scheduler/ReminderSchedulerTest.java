package com.example.aiFocus.scheduler;

import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.repository.FocusBlockRepository;
import com.example.aiFocus.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.Mockito.*;

/**
 * Unit tests for ReminderScheduler class.
 * Tests the reminder sending logic for upcoming focus blocks.
 */
@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock
    private FocusBlockRepository focusBlockRepository;

    @Mock
    private EmailService emailService;

    private ReminderScheduler reminderScheduler;

    @BeforeEach
    void setUp() {
        reminderScheduler = new ReminderScheduler(focusBlockRepository, emailService);
    }

    /**
     * Test that a reminder is sent when a focus block is within 10-20 minutes window.
     */
    @Test
    void sendsReminder_whenBlockIsWithin10To20Minutes() {
        // Arrange
        User user = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .build();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockStartTime = now.plusMinutes(15); // 15 minutes from now

        FocusBlock block = FocusBlock.builder()
            .id(1L)
            .user(user)
            .startTime(blockStartTime)
            .endTime(blockStartTime.plusMinutes(30))
            .status(BlockStatus.SCHEDULED)
            .build();

        when(focusBlockRepository.findByStatus(BlockStatus.SCHEDULED))
            .thenReturn(Arrays.asList(block));

        // Act
        reminderScheduler.checkAndSendReminders();

        // Assert
        verify(emailService, times(1)).sendFocusReminder(user, block);
    }

    /**
     * Test that a reminder is NOT sent when a focus block is too far away.
     */
    @Test
    void doesNotSendReminder_whenBlockIsTooFarAway() {
        // Arrange
        User user = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .build();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockStartTime = now.plusMinutes(60); // 60 minutes from now

        FocusBlock block = FocusBlock.builder()
            .id(1L)
            .user(user)
            .startTime(blockStartTime)
            .endTime(blockStartTime.plusMinutes(30))
            .status(BlockStatus.SCHEDULED)
            .build();

        when(focusBlockRepository.findByStatus(BlockStatus.SCHEDULED))
            .thenReturn(Arrays.asList(block));

        // Act
        reminderScheduler.checkAndSendReminders();

        // Assert
        verify(emailService, never()).sendFocusReminder(user, block);
    }

    /**
     * Test that a reminder is NOT sent when a focus block has already started.
     */
    @Test
    void doesNotSendReminder_whenBlockAlreadyStarted() {
        // Arrange
        User user = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .build();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime blockStartTime = now.minusMinutes(5); // Already started 5 minutes ago

        FocusBlock block = FocusBlock.builder()
            .id(1L)
            .user(user)
            .startTime(blockStartTime)
            .endTime(blockStartTime.plusMinutes(30))
            .status(BlockStatus.SCHEDULED)
            .build();

        when(focusBlockRepository.findByStatus(BlockStatus.SCHEDULED))
            .thenReturn(Arrays.asList(block));

        // Act
        reminderScheduler.checkAndSendReminders();

        // Assert
        verify(emailService, never()).sendFocusReminder(user, block);
    }
}

