CREATE TABLE schema_marker (
                               id BIGINT PRIMARY KEY,
                               marker_name VARCHAR(100) NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO schema_marker (id, marker_name)
VALUES (1, 'baseline');