-- ============================================================
-- BookBridge Migration 004 — Add request_response notification
-- ============================================================

USE bookbridge;

-- Update the ENUM for Notification type to include 'request_response'
ALTER TABLE Notification 
MODIFY COLUMN type ENUM('offer', 'counteroffer', 'message', 'transaction', 'dispute', 'review', 'system', 'request_response') NOT NULL;
