-- Book Bridge Database Schema Definition
-- Target DBMS: MySQL

CREATE DATABASE IF NOT EXISTS bookbridge;
USE bookbridge;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    is_email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Books Table
CREATE TABLE IF NOT EXISTS books (
    book_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    edition VARCHAR(100),
    published_year INT,
    book_condition VARCHAR(50) NOT NULL, -- NEW, LIKE_NEW, GOOD, ACCEPTABLE
    subject VARCHAR(100),
    description TEXT,
    asking_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, RESERVED, SOLD
    seller_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    views_count INT DEFAULT 0,
    CONSTRAINT fk_book_seller FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 2.1 Book Image URLs Table (ElementCollection representation)
CREATE TABLE IF NOT EXISTS book_image_urls (
    book_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    CONSTRAINT fk_book_images FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE
);

-- 3. Wishlists Table
CREATE TABLE IF NOT EXISTS wishlists (
    wishlist_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_book FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_book UNIQUE (user_id, book_id)
);

-- 4. Negotiations Table
CREATE TABLE IF NOT EXISTS negotiations (
    negotiation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, ACCEPTED, REJECTED, EXPIRED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_negotiation_book FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
    CONSTRAINT fk_negotiation_buyer FOREIGN KEY (buyer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_negotiation_seller FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 5. Offers Table
CREATE TABLE IF NOT EXISTS offers (
    offer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    negotiation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    offered_price DECIMAL(10, 2) NOT NULL,
    message VARCHAR(500),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_offer_negotiation FOREIGN KEY (negotiation_id) REFERENCES negotiations(negotiation_id) ON DELETE CASCADE,
    CONSTRAINT fk_offer_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 6. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    negotiation_id BIGINT NOT NULL,
    buyer_id BIGINT NOT NULL,
    seller_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, REFUNDED
    razorpay_order_id VARCHAR(255),
    razorpay_payment_id VARCHAR(255),
    razorpay_signature VARCHAR(255),
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_negotiation FOREIGN KEY (negotiation_id) REFERENCES negotiations(negotiation_id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_buyer FOREIGN KEY (buyer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_transaction_seller FOREIGN KEY (seller_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- 7. Disputes Table
CREATE TABLE IF NOT EXISTS disputes (
    dispute_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- OPEN, IN_REVIEW, RESOLVED, REJECTED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dispute_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
);
