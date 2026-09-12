-- V1__Create_shipping_schema.sql
-- Flyway migration for Shipping Service schema initialization

-- Create ENUM type for shipping status
CREATE TYPE shipping.shipping_status AS ENUM ('PREPARING', 'READY_FOR_SHIPMENT', 'SHIPPED', 'DELIVERED', 'CANCELLED');

-- Create shipments table
CREATE TABLE shipping.shipment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,     -- Reference to orders.order (no FK!)
    user_id UUID NOT NULL,      -- Reference to app_user.user (no FK!)
    shipping_status shipping.shipping_status NOT NULL DEFAULT 'PREPARING',
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

