-- V1__create_users_table.sql
-- Creates the users table as the foundation for the Focus Time Optimizer application.
-- Stores user account information and authentication details.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    timezone VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create index on email for faster lookups during authentication
CREATE INDEX idx_users_email ON users(email);

