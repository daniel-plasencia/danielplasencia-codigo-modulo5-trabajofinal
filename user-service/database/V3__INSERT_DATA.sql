-- ============================================
-- Migration: V3__INSERT_DATA.sql
-- Passwords BCrypt:
-- admin123 → $2a$10$e77InV9/.OZ68nmbd9Co2uhuYu9g7eBNqu3nDyRHcC5x0cIH0YBJW
-- user123  → $2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu
-- ============================================

INSERT INTO users (name, email, phone, address, password, role) VALUES
('Juan Pérez', 'juan.perez@example.com', '+51-999-123-456', 'Av. Arequipa 1234, Lima', 
 '$2a$10$e77InV9/.OZ68nmbd9Co2uhuYu9g7eBNqu3nDyRHcC5x0cIH0YBJW', 'ADMIN'),
('María García', 'maria.garcia@example.com', '+51-999-234-567', 'Calle Los Olivos 567, Miraflores', 
 '$2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu', 'USER'),
('Carlos López', 'carlos.lopez@example.com', '+51-999-345-678', 'Jr. Huancavelica 890, Lima Centro', 
 '$2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu', 'USER'),
('Ana Torres', 'ana.torres@example.com', '+51-999-456-789', 'Av. Javier Prado 2345, San Isidro', 
 '$2a$10$e77InV9/.OZ68nmbd9Co2uhuYu9g7eBNqu3nDyRHcC5x0cIH0YBJW', 'ADMIN'),
('Roberto Sánchez', 'roberto.sanchez@example.com', '+51-999-567-890', 'Calle Las Begonias 678, San Borja', 
 '$2a$10$tQWrbvoAohyaYiDC6e9rNO9Wf7w0eLQamxD2TJhCWKXbJjqjRTXUu', 'USER');