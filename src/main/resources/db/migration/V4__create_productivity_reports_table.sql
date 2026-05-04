-- V4__create_productivity_reports_table.sql
-- Creates the productivity_reports table to store weekly productivity analytics.
-- Generates AI-driven summaries and insights for focus time optimization.

CREATE TABLE productivity_reports (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    week_start DATE NOT NULL,
    total_focus_minutes INT,
    blocks_completed INT,
    blocks_skipped INT,
    peak_hour VARCHAR(5),
    ai_summary TEXT,
    generated_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for efficient queries
CREATE INDEX idx_productivity_reports_user_id ON productivity_reports(user_id);
CREATE INDEX idx_productivity_reports_week_start ON productivity_reports(week_start);

