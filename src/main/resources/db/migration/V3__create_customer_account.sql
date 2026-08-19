CREATE TABLE customer_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT uq_customer_account_name
        UNIQUE (customer_id, name),

    CONSTRAINT fk_customer_account_customer
        FOREIGN KEY (customer_id)
        REFERENCES customer (id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_customer_account_status
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),

    CONSTRAINT chk_customer_account_currency
        CHECK (currency = 'MYR')
);

CREATE INDEX idx_customer_account_status
    ON customer_account (status);