-- Create initial admin user
-- Password: admin123 (BCrypt encoded)
INSERT INTO users (username, name, email, password, role, active, created_at, last_login)
VALUES ('admin', 'System Administrator', 'admin@rms.local', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5NyYlWfWjUXzm', 'ADMIN', true, NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;
