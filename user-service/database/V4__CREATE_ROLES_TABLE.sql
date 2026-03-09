-- ============================================
-- Migration: V4__CREATE_ROLES_TABLE
-- Description: Tabla roles requerida por RoleEntity
-- ============================================

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Datos iniciales para compatibilidad con users.role (ADMIN, USER)
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrador del sistema'),
('USER', 'Usuario estándar')
ON CONFLICT (name) DO NOTHING;
