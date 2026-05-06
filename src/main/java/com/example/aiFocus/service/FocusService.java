package com.example.aiFocus.service;

import com.example.aiFocus.dto.BlockRequest;
import com.example.aiFocus.dto.FocusSuggestion;
import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.CalendarEvent;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.event.FocusBlockScheduledEvent;
import com.example.aiFocus.exception.ResourceNotFoundException;
import com.example.aiFocus.repository.FocusBlockRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing focus blocks and AI-generated suggestions.
 * Handles creation, updating, and retrieval of focus blocks, as well as
 * generating AI-powered suggestions for optimal focus times.
 */
@Service
public class FocusService {

    private final FocusBlockRepository focusBlockRepository;
    private final CalendarService calendarService;
    private final AiService aiService;
    private final PromptBuilder promptBuilder;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs a new FocusService with the required dependencies.
     *
     * @param focusBlockRepository the repository for focus blocks
     * @param calendarService the service for calendar operations
     * @param aiService the service for AI interactions
     * @param promptBuilder the service for building AI prompts
     * @param eventPublisher the publisher for application events
     */
    public FocusService(FocusBlockRepository focusBlockRepository, CalendarService calendarService,
                        AiService aiService, PromptBuilder promptBuilder, ApplicationEventPublisher eventPublisher) {
        this.focusBlockRepository = focusBlockRepository;
        this.calendarService = calendarService;
        this.aiService = aiService;
        this.promptBuilder = promptBuilder;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Generates AI-powered focus block suggestions for the user.
     *
     * @param user the user for whom suggestions are generated
     * @return a list of focus suggestions
     */
    public List<FocusSuggestion> getSuggestions(User user) {
        List<CalendarEvent> todayEvents = calendarService.getTodayEvents(user);
        List<FocusBlock> recentBlocks = focusBlockRepository.findByUserAndStatus(user, BlockStatus.COMPLETED);
        // Filter to last 2 weeks
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
        recentBlocks = recentBlocks.stream()
                .filter(block -> block.getCreatedAt().isAfter(twoWeeksAgo))
                .toList();

        String prompt = promptBuilder.buildPrompt(user, todayEvents, recentBlocks);
        return aiService.getSuggestions(prompt);
    }

    /**
     * Confirms and saves a focus block based on a user request.
     *
     * @param user the user confirming the block
     * @param request the block request details
     * @return the saved focus block
     */
    public FocusBlock confirmBlock(User user, BlockRequest request) {
        LocalDateTime start = request.start().toLocalDateTime();
        LocalDateTime end   = request.end().toLocalDateTime();

        FocusBlock focusBlock = FocusBlock.builder()
                .user(user)
                .startTime(start)
                .endTime(end)
                .status(BlockStatus.SCHEDULED)
                .aiConfidenceScore(request.confidence())
                .build();

        FocusBlock saved = focusBlockRepository.save(focusBlock);
        eventPublisher.publishEvent(new FocusBlockScheduledEvent(saved));
        return saved;
    }

    /**
     * Updates the status of a focus block.
     *
     * @param blockId the ID of the block to update
     * @param status the new status
     * @param user the user performing the update
     * @return the updated focus block
     * @throws ResourceNotFoundException if the block is not found or does not belong to the user
     */
    public FocusBlock updateStatus(Long blockId, BlockStatus status, User user) {
        FocusBlock focusBlock = focusBlockRepository.findById(blockId)
                .orElseThrow(() -> new ResourceNotFoundException("Focus block not found with id: " + blockId));

        if (!focusBlock.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Focus block not found with id: " + blockId);
        }

        focusBlock.setStatus(status);
        return focusBlockRepository.save(focusBlock);
    }

    /**
     * Retrieves all focus blocks for a user.
     *
     * @param user the user whose blocks to retrieve
     * @return a list of focus blocks
     */
    public List<FocusBlock> getBlocks(User user) {
        return focusBlockRepository.findByUser(user);
    }
}
