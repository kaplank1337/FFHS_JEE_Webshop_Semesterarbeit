-- V1__Create_user_schema.sql
-- Flyway migration for User Service schema initialization

-- Create ENUM type for user roles
CREATE TYPE app_user.user_role AS ENUM ('CUSTOMER', 'ADMIN');

-- Create users table
CREATE TABLE app_user."user" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role app_user.user_role NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX idx_user_username ON app_user."user"(username);
CREATE INDEX idx_user_email ON app_user."user"(email);
CREATE INDEX idx_user_role ON app_user."user"(role);
CREATE INDEX idx_user_created_at ON app_user."user"(created_at DESC);

