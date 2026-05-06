package com.example.aiFocus.controller;

import com.example.aiFocus.dto.BlockRequest;
import com.example.aiFocus.dto.BlockStatusUpdate;
import com.example.aiFocus.dto.FocusSuggestion;
import com.example.aiFocus.entity.BlockStatus;
import com.example.aiFocus.entity.FocusBlock;
import com.example.aiFocus.entity.User;
import com.example.aiFocus.service.FocusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing focus blocks and AI suggestions.
 * Provides endpoints for generating suggestions, confirming blocks,
 * updating block status, and retrieving user blocks.
 */
@RestController
@RequestMapping("/api/focus")
@Validated
public class FocusController {

    private final FocusService focusService;

    /**
     * Constructs a new FocusController with the required service.
     *
     * @param focusService the focus service
     */
    public FocusController(FocusService focusService) {
        this.focusService = focusService;
    }

    /**
     * Gets the current authenticated user from the security context.
     *
     * @return the current user
     */
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Generates AI-powered focus block suggestions for the current user.
     *
     * @return a list of focus suggestions
     */
    @GetMapping("/suggest")
    public ResponseEntity<List<FocusSuggestion>> getSuggestions() {
        User user = getCurrentUser();
        List<FocusSuggestion> suggestions = focusService.getSuggestions(user);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Confirms and creates a new focus block based on the provided request.
     *
     * @param request the block request details
     * @return the created focus block
     */
    @PostMapping("/block")
    public ResponseEntity<FocusBlock> confirmBlock(@Valid @RequestBody BlockRequest request) {
        User user = getCurrentUser();
        FocusBlock focusBlock = focusService.confirmBlock(user, request);
        return ResponseEntity.status(201).body(focusBlock);
    }

    /**
     * Retrieves all focus blocks for the current user.
     *
     * @return a list of focus blocks
     */
    @GetMapping("/blocks")
    public ResponseEntity<List<FocusBlock>> getBlocks() {
        User user = getCurrentUser();
        List<FocusBlock> blocks = focusService.getBlocks(user);
        return ResponseEntity.ok(blocks);
    }

    /**
     * Updates the status of a specific focus block.
     *
     * @param id the ID of the block to update
     * @param update the status update request
     * @return the updated focus block
     */
    @PatchMapping("/blocks/{id}/status")
    public ResponseEntity<FocusBlock> updateBlockStatus(@PathVariable Long id, @Valid @RequestBody BlockStatusUpdate update) {
        User user = getCurrentUser();
        FocusBlock updatedBlock = focusService.updateStatus(id, update.status(), user);
        return ResponseEntity.ok(updatedBlock);
    }
}
