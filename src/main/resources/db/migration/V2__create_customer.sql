CREATE TABLE customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uq_customer_email UNIQUE (email),
    CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
);

CREATE INDEX idx_customer_status ON customer (status);