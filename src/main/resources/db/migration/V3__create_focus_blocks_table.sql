-- V3__create_focus_blocks_table.sql
-- Creates the focus_blocks table to record focused work sessions.
-- Stores AI confidence scores and status tracking for focus time optimization.

CREATE TABLE focus_blocks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    ai_confidence_score DECIMAL(3, 2),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for efficient queries
CREATE INDEX idx_focus_blocks_user_id ON focus_blocks(user_id);
CREATE INDEX idx_focus_blocks_start_time ON focus_blocks(start_time);
CREATE INDEX idx_focus_blocks_status ON focus_blocks(status);

