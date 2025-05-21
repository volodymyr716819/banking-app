-- Fix card_details table structure
DROP TABLE IF EXISTS card_details;

-- Create a properly structured card_details table
CREATE TABLE card_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    hashed_pin VARCHAR(255) NOT NULL DEFAULT '',
    pin_created BOOLEAN DEFAULT FALSE NOT NULL,
    last_pin_changed TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);