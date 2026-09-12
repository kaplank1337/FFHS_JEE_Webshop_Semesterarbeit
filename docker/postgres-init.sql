-- PostgreSQL Initialization Script
-- Creates three separate schemas for microservices

-- Create schemas
CREATE SCHEMA IF NOT EXISTS app_user;
CREATE SCHEMA IF NOT EXISTS orders;
CREATE SCHEMA IF NOT EXISTS shipping;

-- Grant privileges to webshop user
GRANT ALL PRIVILEGES ON SCHEMA app_user TO webshop;
GRANT ALL PRIVILEGES ON SCHEMA orders TO webshop;
GRANT ALL PRIVILEGES ON SCHEMA shipping TO webshop;

-- Set search path for webshop user (optional, for convenience)
ALTER USER webshop SET search_path = public, app_user, orders, shipping;

-- Create extension for UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant extension privileges
GRANT USAGE ON SCHEMA public TO webshop;

