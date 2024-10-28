CREATE TABLE IF NOT EXISTS rules (
    id INT PRIMARY KEY,
    name VARCHAR(255),
    condition VARCHAR(255),
    action VARCHAR(255)
);