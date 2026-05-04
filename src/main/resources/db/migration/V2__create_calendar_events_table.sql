-- V2__create_calendar_events_table.sql
-- Creates the calendar_events table to store calendar events imported from various sources.
-- Tracks event metadata and synchronization status for AI analysis.

CREATE TABLE calendar_events (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    type VARCHAR(20),
    source VARCHAR(20),
    synced_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes for faster queries
CREATE INDEX idx_calendar_events_user_id ON calendar_events(user_id);
CREATE INDEX idx_calendar_events_start_time ON calendar_events(start_time);
CREATE INDEX idx_calendar_events_end_time ON calendar_events(end_time);

