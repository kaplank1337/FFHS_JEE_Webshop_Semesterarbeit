ALTER TABLE orders."order"
    DROP CONSTRAINT IF EXISTS order_status_check;

ALTER TABLE orders."order"
    ADD CONSTRAINT order_status_check
        CHECK (status IN ('CREATED', 'CONFIRMED', 'SHIPPING', 'SHIPPED', 'DELIVERED', 'CANCELLED'));
