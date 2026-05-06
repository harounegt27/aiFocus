package com.example.aiFocus.service;

import com.example.aiFocus.entity.CalendarEvent;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service responsible for building prompts for AI suggestions.
 * Constructs detailed prompts that include user context, today's events,
 * and recent focus blocks to generate optimal focus time suggestions.
 */
@Service
public class PromptBuilder {

    /**
     * Builds a prompt for the AI model to generate focus block suggestions.
     *
     * @param user the user for whom suggestions are being generated
     * @param todayEvents list of calendar events for today
     * @param recentBlocks list of completed focus blocks from the last 2 weeks
     * @return a string prompt instructing the AI to return a raw JSON array of suggestions
     */
    public String buildPrompt(User user, List<CalendarEvent> todayEvents, List<FocusBlock> recentBlocks) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI assistant that helps users optimize their focus time. ");
        prompt.append("Based on the user's timezone, today's date, their calendar events, and recent focus blocks, ");
        prompt.append("suggest optimal focus blocks for today.\n\n");

        // User timezone and today's date
        prompt.append("User Timezone: ").append(user.getTimezone() != null ? user.getTimezone() : "UTC").append("\n");
        prompt.append("Today's Date: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");

        // Today's meetings
        prompt.append("Today's Meetings:\n");
        if (todayEvents.isEmpty()) {
            prompt.append("- No meetings scheduled\n");
        } else {
            for (CalendarEvent event : todayEvents) {
                prompt.append("- ").append(event.getTitle())
                      .append(" (").append(event.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                      .append(" to ").append(event.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                      .append(")\n");
            }
        }
        prompt.append("\n");

        // Recent completed blocks
        prompt.append("Recent Completed Focus Blocks (last 2 weeks):\n");
        if (recentBlocks.isEmpty()) {
            prompt.append("- No recent completed blocks\n");
        } else {
            for (FocusBlock block : recentBlocks) {
                prompt.append("- ").append(block.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                      .append(" to ").append(block.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                      .append(" (confidence: ").append(block.getAiConfidenceScore()).append(")\n");
            }
        }
        prompt.append("\n");

        // Instructions
        prompt.append("Instructions:\n");
        prompt.append("- Suggest 2-4 focus blocks of 1-2 hours each\n");
        prompt.append("- Avoid times overlapping with meetings\n");
        prompt.append("- Consider typical work hours (9 AM - 6 PM in user's timezone)\n");
        prompt.append("- Provide high confidence scores (0.7-1.0) for optimal times\n");
        prompt.append("- Include a brief reason for each suggestion\n\n");

        prompt.append("Return ONLY a raw JSON array with no markdown, code fences, or explanation. ");
        prompt.append("Format: [{\"start\": \"ISO datetime\", \"end\": \"ISO datetime\", \"confidence\": 0.0, \"reason\": \"string\"}, ...]");

        return prompt.toString();
    }
}
