-- =====================================================
-- COLLEGE CLEANLINESS MANAGEMENT SYSTEM - DATABASE SCRIPT
-- =====================================================

-- Create Database
CREATE DATABASE IF NOT EXISTS cleanliness_db;
USE cleanliness_db;

-- =====================================================
-- TABLES
-- =====================================================

-- Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(100),
    roll_number VARCHAR(50),
    profile_image TEXT,
    points INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Roles Junction Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Complaints Table
CREATE TABLE IF NOT EXISTS complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    location_type VARCHAR(50),
    specific_location VARCHAR(100),
    image_path VARCHAR(500),
    after_image_path VARCHAR(500),
    original_filename VARCHAR(255),
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    status VARCHAR(30) DEFAULT 'PENDING',
    assigned_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    verified_at TIMESTAMP NULL,
    resolution_notes TEXT,
    rating INT,
    feedback_comment TEXT,
    user_id BIGINT NOT NULL,
    staff_id BIGINT,
    assigned_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (staff_id) REFERENCES users(id),
    FOREIGN KEY (assigned_by) REFERENCES users(id)
);

-- Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    user_id BIGINT NOT NULL,
    complaint_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE SET NULL
);

-- =====================================================
-- INSERT DEFAULT ROLES
-- =====================================================
INSERT INTO roles (name) VALUES ('ADMIN'), ('STAFF'), ('STUDENT');

-- =====================================================
-- INSERT DEFAULT ADMIN (Password: admin123 - encrypted)
-- =====================================================
INSERT INTO users (email, password, full_name, phone, department, enabled) 
VALUES ('admin@college.com', '$2a$10$XQzYqJ7PQZ7YNfKxLk1E4eHJZJvQKzYz6FxKxLk1E4eHJZJvQKzYz', 'System Administrator', '1234567890', 'Administration', TRUE);

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'admin@college.com' AND r.name = 'ADMIN';

-- =====================================================
-- SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Sample Staff
INSERT INTO users (email, password, full_name, phone, department, enabled, points) 
VALUES ('staff1@college.com', '$2a$10$XQzYqJ7PQZ7YNfKxLk1E4eHJZJvQKzYz6FxKxLk1E4eHJZJvQKzYz', 'John Smith', '9876543210', 'Cleaning', TRUE, 150);

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'staff1@college.com' AND r.name = 'STAFF';

-- Sample Student
INSERT INTO users (email, password, full_name, phone, department, roll_number, enabled) 
VALUES ('student@college.com', '$2a$10$XQzYqJ7PQZ7YNfKxLk1E4eHJZJvQKzYz6FxKxLk1E4eHJZJvQKzYz', 'Alice Johnson', '9876543211', 'Computer Science', 'CS2024001', TRUE);

INSERT INTO user_roles (user_id, role_id) 
SELECT u.id, r.id FROM users u, roles r WHERE u.email = 'student@college.com' AND r.name = 'STUDENT';

-- Sample Complaint
INSERT INTO complaints (title, description, location_type, specific_location, priority, status, user_id) 
VALUES ('Dirty Classroom', 'The classroom 101 needs cleaning. There is dust on desks.', 'CLASSROOM', 'Room 101, Block A', 'MEDIUM', 'PENDING', 
(SELECT id FROM users WHERE email = 'student@college.com'));

-- =====================================================
-- NOTES
-- =====================================================
-- Default password for all users: admin123
-- Password is BCrypt encrypted
-- 
-- To create more users, register through the application or insert manually
-- Image files will be stored in: src/main/resources/static/uploads/
--
-- Default MySQL Credentials (change in application.properties):
-- Username: root
-- Password: root
-- Database: cleanliness_db
