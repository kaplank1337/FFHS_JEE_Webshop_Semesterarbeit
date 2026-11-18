-- V1__Create_shipping_schema.sql
-- Flyway migration for Shipping Service schema initialization

CREATE SCHEMA IF NOT EXISTS shipping;

-- Create shipments table
CREATE TABLE shipping.shipment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,     -- Reference to orders.order (no FK!)
    user_id UUID NOT NULL,      -- Reference to app_user.user (no FK!)
    shipping_status VARCHAR(30) NOT NULL DEFAULT 'PREPARING'
        CHECK (shipping_status IN ('PREPARING', 'READY_FOR_SHIPMENT', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    tracking_number VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX idx_shipment_order_id ON shipping.shipment(order_id);
CREATE INDEX idx_shipment_user_id ON shipping.shipment(user_id);
CREATE INDEX idx_shipment_status ON shipping.shipment(shipping_status);
CREATE INDEX idx_shipment_created_at ON shipping.shipment(created_at DESC);
CREATE INDEX idx_shipment_tracking ON shipping.shipment(tracking_number);

