-- V2__Insert_admin_user.sql
-- Seed data: Insert admin user

-- PBKDF2 hash for password "admin123" (generated with PasswordEncoder)
-- Note: In production, this should be generated securely
INSERT INTO app_user."user" (
    id,
    username,
    email,
    password_hash,
    first_name,
    last_name,
    role,
    created_at
) VALUES (
    '550e8400-e29b-41d4-a716-446655440000'::UUID,
    'admin',
    'admin@webshop.local',
    'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA+o3RvXPLZYKIuCnJQqAE4Pq4Y1pFZcNJ/zZpBdQzNQzh9GDjt5cpZKfpvHv3Y6dJvP8Fw==',
    'Admin',
    'User',
    'ADMIN'::app_user.user_role,
    CURRENT_TIMESTAMP
);

