-- Migration to remove 'Exchange' and 'Free/Donation' features

-- 1. Drop foreign key constraint on Negotiation table for offered_book_id
-- Depending on how the database was created, the constraint name might vary.
-- If the constraint is explicitly named, you can drop it directly. 
-- Otherwise, you must identify the constraint name first:
-- SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'Negotiation' AND COLUMN_NAME = 'offered_book_id';
-- ALTER TABLE Negotiation DROP FOREIGN KEY <constraint_name>;
-- We include a common default name here:
ALTER TABLE Negotiation DROP FOREIGN KEY fk_negotiation_offered_book;

-- 2. Drop offered_book_id from Negotiation table
ALTER TABLE Negotiation DROP COLUMN offered_book_id;

-- 3. Drop type and exchange_for from Book table
ALTER TABLE Book DROP COLUMN type, DROP COLUMN exchange_for;
