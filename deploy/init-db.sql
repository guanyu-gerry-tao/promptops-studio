-- PromptOps Studio - Database Initialization Script
-- Milestone 2: User and Project Management

-- Drop tables if exist (for clean reinstall)
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS users;

-- ==============================================
-- Table: users
-- Description: User accounts and authentication
-- ==============================================
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'User ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT 'Username (unique)',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT 'Email address (unique)',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Password hash (BCrypt)',
    display_name VARCHAR(100) COMMENT 'Display name',
    avatar_url VARCHAR(500) COMMENT 'Avatar URL',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE, INACTIVE, BANNED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',

    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts';

-- ==============================================
-- Table: projects
-- Description: User projects (isolated workspaces)
-- ==============================================
CREATE TABLE projects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Project ID',
    name VARCHAR(100) NOT NULL COMMENT 'Project name',
    description TEXT COMMENT 'Project description',
    owner_id BIGINT NOT NULL COMMENT 'Owner user ID',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE, ARCHIVED, DELETED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',

    INDEX idx_owner_id (owner_id),
    INDEX idx_status (status),
    INDEX idx_name (name),
    INDEX idx_created_at (created_at),

    CONSTRAINT fk_project_owner
        FOREIGN KEY (owner_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User projects';

-- ==============================================
-- Table: audit_logs
-- Description: Audit trail for all operations
-- ==============================================
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Log ID',
    user_id BIGINT COMMENT 'Operating user ID (nullable for system operations)',
    action VARCHAR(50) NOT NULL COMMENT 'Action type: CREATE, UPDATE, DELETE, LOGIN, LOGOUT',
    resource_type VARCHAR(50) COMMENT 'Resource type: USER, PROJECT, KB, WORKFLOW, DATASET, RUN',
    resource_id BIGINT COMMENT 'Resource ID',
    details JSON COMMENT 'Additional details in JSON format',
    ip_address VARCHAR(45) COMMENT 'IP address (supports IPv6)',
    user_agent VARCHAR(500) COMMENT 'User agent string',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Operation timestamp',

    INDEX idx_user_id (user_id),
    INDEX idx_action (action),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_created_at (created_at),

    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Audit logs';

-- ==============================================
-- Table: kb_docs
-- Description: Knowledge base document metadata.
--   Stores which documents have been uploaded and
--   indexed for each project. The actual document
--   content (chunks + embeddings) lives in Milvus,
--   not in this table.
-- ==============================================
CREATE TABLE kb_docs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Document ID',
    project_id BIGINT NOT NULL COMMENT 'Which project this doc belongs to',
    title VARCHAR(255) NOT NULL COMMENT 'Document title',
    content MEDIUMTEXT COMMENT 'Original document content (Markdown)',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'Index status: PENDING, INDEXING, INDEXED, FAILED',
    chunks_count INT DEFAULT 0 COMMENT 'Number of chunks created in Milvus',
    error_message TEXT COMMENT 'Error details if indexing failed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload timestamp',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',

    INDEX idx_project_id (project_id),
    INDEX idx_status (status),

    CONSTRAINT fk_kbdoc_project
        FOREIGN KEY (project_id)
        REFERENCES projects(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge base documents';

-- ==============================================
-- Insert test data (for development)
-- ==============================================

-- Test user (password: "password123")
-- BCrypt hash generated with strength 10
INSERT INTO users (username, email, password_hash, display_name, status) VALUES
('admin', 'admin@promptops.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'System Admin', 'ACTIVE'),
('testuser', 'test@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'Test User', 'ACTIVE');

-- Test projects
INSERT INTO projects (name, description, owner_id, status) VALUES
('Demo Project', 'This is a demo project for testing', 1, 'ACTIVE'),
('AI Assistant', 'AI-powered customer support assistant', 1, 'ACTIVE'),
('Test Project', 'Testing workspace', 2, 'ACTIVE');

-- Test audit logs
INSERT INTO audit_logs (user_id, action, resource_type, resource_id, details, ip_address) VALUES
(1, 'LOGIN', 'USER', 1, '{"success": true}', '127.0.0.1'),
(1, 'CREATE', 'PROJECT', 1, '{"name": "Demo Project"}', '127.0.0.1'),
(2, 'LOGIN', 'USER', 2, '{"success": true}', '127.0.0.1'),
(2, 'CREATE', 'PROJECT', 3, '{"name": "Test Project"}', '127.0.0.1');

-- ==============================================
-- Verify tables
-- ==============================================
SELECT 'Database initialized successfully!' AS status;
SELECT TABLE_NAME, TABLE_COMMENT
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME;
