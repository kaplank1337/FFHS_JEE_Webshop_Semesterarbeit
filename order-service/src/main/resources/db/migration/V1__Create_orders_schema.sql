-- V1__Create_orders_schema.sql
-- Flyway migration for Order Service schema initialization

CREATE SCHEMA IF NOT EXISTS orders;

-- Create products table
CREATE TABLE orders.product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(50) NOT NULL CHECK (category IN ('WATER', 'SOFT_DRINKS', 'JUICE', 'BEER', 'COFFEE')),
    price NUMERIC(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create orders table
CREATE TABLE orders."order" (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,  -- Reference to app_user.user (no FK!)
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED'
        CHECK (status IN ('CREATED', 'CONFIRMED', 'SHIPPING', 'SHIPPED', 'CANCELLED')),
    total_price NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create order_items table
CREATE TABLE orders.order_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders."order"(id),
    product_id UUID NOT NULL,  -- Reference to product (no bidirectional FK!)
    product_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    total_price NUMERIC(10, 2) NOT NULL
);

-- Create indexes for common queries
CREATE INDEX idx_product_active ON orders.product(active);
CREATE INDEX idx_product_category ON orders.product(category);
CREATE INDEX idx_product_created_at ON orders.product(created_at DESC);

CREATE INDEX idx_order_user_id ON orders."order"(user_id);
CREATE INDEX idx_order_status ON orders."order"(status);
CREATE INDEX idx_order_created_at ON orders."order"(created_at DESC);

CREATE INDEX idx_order_item_order ON orders.order_item(order_id);
CREATE INDEX idx_order_item_product ON orders.order_item(product_id);

