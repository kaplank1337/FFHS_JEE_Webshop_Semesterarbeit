-- V2__Insert_products.sql
-- Seed data: Insert default beverage products

INSERT INTO orders.product (id, name, description, category, price, stock_quantity, active, created_at)
VALUES
('550e8400-e29b-41d4-a716-446655440011'::UUID, 'Mineralwasser 1.5L', 'Natürliches Mineralwasser in 1.5L Flasche', 'WATER'::orders.product_category, 1.49, 100, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440012'::UUID, 'Mineralwasser 0.5L', 'Natürliches Mineralwasser in 0.5L Flasche', 'WATER'::orders.product_category, 0.79, 150, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440013'::UUID, 'Cola 0.5L', 'Erfrischendes Cola Getränk', 'SOFT_DRINKS'::orders.product_category, 2.49, 80, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440014'::UUID, 'Orangensaft 1L', '100% gepresster Orangensaft', 'JUICE'::orders.product_category, 3.99, 60, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440015'::UUID, 'Eistee 0.5L', 'Erfrischender Eistee mit Zitrone', 'SOFT_DRINKS'::orders.product_category, 2.29, 70, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440016'::UUID, 'Lager Bier 0.5L', 'Klassisches Lagerbier', 'BEER'::orders.product_category, 1.99, 120, true, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440017'::UUID, 'Espresso Cold Brew', 'Kalt gebrühter Espresso im 0.33L Format', 'COFFEE'::orders.product_category, 3.49, 50, true, CURRENT_TIMESTAMP);

