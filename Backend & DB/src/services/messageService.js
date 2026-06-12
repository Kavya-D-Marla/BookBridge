/**
 * Message Service
 * Aligned with frontend schema format
 */

const { pool } = require('../config/db');
const AppError   = require('../utils/AppError');
const { PAGINATION } = require('../config/constants');
const { createNotification } = require('./notificationService');

// ─── Internal Helpers ──────────────────────────────────────────────────────

const getMessageById = async (messageId) => {
  const [rows] = await pool.query(
    `SELECT m.*,
            s.user_name AS sender_name,   s.profile_picture AS sender_picture,
            r.user_name AS receiver_name, r.profile_picture AS receiver_picture
     FROM Message m
     JOIN User s ON m.sender_id   = s.user_id
     JOIN User r ON m.receiver_id = r.user_id
     WHERE m.message_id = ?`,
    [messageId]
  );

  if (rows.length === 0) {
    throw AppError.notFound('Message not found');
  }

  return rows[0];
};

// ─── Public Service Methods ────────────────────────────────────────────────

const sendMessage = async (senderId, receiverId, content, negotiationId = null) => {
  if (senderId === receiverId) {
    throw AppError.badRequest('You cannot send a message to yourself');
  }

  const [receiverRows] = await pool.query(
    'SELECT user_id FROM User WHERE user_id = ?',
    [receiverId]
  );

  if (receiverRows.length === 0) {
    throw AppError.notFound('Recipient user not found');
  }

  const [result] = await pool.query(
    `INSERT INTO Message (sender_id, receiver_id, negotiation_id, content, is_read)
     VALUES (?, ?, ?, ?, FALSE)`,
    [senderId, receiverId, negotiationId, content]
  );

  const messageRecord = await getMessageById(result.insertId);

  await createNotification(
    receiverId,
    'message',
    `New message from ${messageRecord.sender_name}`,
    result.insertId,
    'message'
  );

  return messageRecord;
};

const getConversations = async (userId) => {
  const [rows] = await pool.query(
    `SELECT
       partner.user_id           AS partner_id,
       partner.user_name         AS partner_name,
       partner.profile_picture   AS partner_picture,
       partner.role              AS partner_role,
       latest.content            AS last_message,
       latest.created_at         AS last_message_at,
       latest.sender_id          AS last_sender_id,
       COALESCE(unread.cnt, 0)   AS unread_count,
       b.book_id,
       b.title                   AS book_title,
       b.asking_price            AS book_asking_price,
       b.image_url               AS book_image
     FROM (
       SELECT DISTINCT
         CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS partner_id
       FROM Message
       WHERE sender_id = ? OR receiver_id = ?
     ) AS conv
     JOIN User partner ON partner.user_id = conv.partner_id
     JOIN Message latest ON latest.message_id = (
       SELECT message_id
       FROM Message
       WHERE (sender_id = ? AND receiver_id = conv.partner_id)
          OR (sender_id = conv.partner_id AND receiver_id = ?)
       ORDER BY created_at DESC, message_id DESC
       LIMIT 1
     )
     LEFT JOIN Negotiation n ON COALESCE(latest.negotiation_id, 
       (SELECT negotiation_id FROM Negotiation 
        WHERE (buyer_id = ? AND book_id IN (SELECT book_id FROM Book WHERE seller_id = conv.partner_id))
           OR (buyer_id = conv.partner_id AND book_id IN (SELECT book_id FROM Book WHERE seller_id = ?))
        ORDER BY updated_at DESC LIMIT 1)
     ) = n.negotiation_id
     LEFT JOIN Book b ON n.book_id = b.book_id
     LEFT JOIN (
       SELECT sender_id, COUNT(*) AS cnt
       FROM Message
       WHERE receiver_id = ? AND is_read = FALSE
       GROUP BY sender_id
     ) AS unread ON unread.sender_id = conv.partner_id
     ORDER BY last_message_at DESC`,
    [userId, userId, userId, userId, userId, userId, userId, userId, userId]
  );

  return rows;
};

const getConversationHistory = async (
  currentUserId,
  otherUserId,
  page = PAGINATION.DEFAULT_PAGE,
  limit = PAGINATION.DEFAULT_LIMIT
) => {
  const queryLimit = Math.min(parseInt(limit, 10), PAGINATION.MAX_LIMIT);
  const offset = (parseInt(page, 10) - 1) * queryLimit;

  const [otherRows] = await pool.query(
    'SELECT user_id, user_name, profile_picture, role FROM User WHERE user_id = ?',
    [otherUserId]
  );

  if (otherRows.length === 0) {
    throw AppError.notFound('User not found');
  }

  const [countRows] = await pool.query(
    `SELECT COUNT(*) AS total FROM Message
     WHERE (sender_id = ? AND receiver_id = ?)
        OR (sender_id = ? AND receiver_id = ?)`,
    [currentUserId, otherUserId, otherUserId, currentUserId]
  );

  const total = countRows[0].total;

  const [messages] = await pool.query(
    `SELECT
       m.message_id,
       m.sender_id,
       m.receiver_id,
       m.negotiation_id,
       m.content,
       m.is_read,
       m.created_at,
       s.user_name AS sender_name,
       s.profile_picture AS sender_picture
     FROM Message m
     JOIN User s ON m.sender_id = s.user_id
     WHERE (m.sender_id = ? AND m.receiver_id = ?)
        OR (m.sender_id = ? AND m.receiver_id = ?)
     ORDER BY m.created_at ASC, m.message_id ASC
     LIMIT ${parseInt(queryLimit, 10)} OFFSET ${parseInt(offset, 10)}`,
    [currentUserId, otherUserId, otherUserId, currentUserId]
  );

  return {
    partner: otherRows[0],
    messages,
    pagination: {
      total,
      page: parseInt(page, 10),
      limit: queryLimit,
      totalPages: Math.ceil(total / queryLimit),
    },
  };
};

const markMessageRead = async (messageId, userId) => {
  const message = await getMessageById(messageId);

  if (message.receiver_id !== userId) {
    throw AppError.forbidden('You can only mark your own received messages as read');
  }

  if (message.is_read) {
    return message;
  }

  await pool.query(
    'UPDATE Message SET is_read = TRUE WHERE message_id = ?',
    [messageId]
  );

  return getMessageById(messageId);
};

const getUnreadCount = async (userId) => {
  const [rows] = await pool.query(
    'SELECT COUNT(*) AS unreadCount FROM Message WHERE receiver_id = ? AND is_read = FALSE',
    [userId]
  );

  return { unreadCount: rows[0].unreadCount };
};

module.exports = {
  sendMessage,
  getConversations,
  getConversationHistory,
  markMessageRead,
  getUnreadCount,
};
