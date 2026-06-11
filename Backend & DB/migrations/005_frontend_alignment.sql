-- Align Book table with frontend
ALTER TABLE Book ADD COLUMN type ENUM('Sell', 'Exchange', 'Free') DEFAULT 'Sell';
ALTER TABLE Book ADD COLUMN exchange_for VARCHAR(255) DEFAULT NULL;

-- Align Negotiation/Offers table with frontend
ALTER TABLE Negotiation ADD COLUMN offered_book_id INT DEFAULT NULL;
ALTER TABLE Negotiation ADD CONSTRAINT fk_negotiation_offered_book FOREIGN KEY (offered_book_id) REFERENCES Book(book_id) ON DELETE SET NULL;

ALTER TABLE Offers ADD COLUMN message TEXT DEFAULT NULL;
