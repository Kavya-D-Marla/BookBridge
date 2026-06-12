/**
 * Message Controller
 * HTTP handlers for messaging endpoints.
 * All business logic delegated to messageService.
 */

const messageService = require('../services/messageService');
const { pool } = require('../config/db');

/**
 * Send a new message
 * @route POST /api/messages
 * @access Private
 */
const sendMessage = async (req, res, next) => {
  try {
    const senderId = req.user.user_id;
    let { receiverId, content, negotiationId = null, bookId = null } = req.body;

    // Resolve negotiationId from bookId if not explicitly supplied
    if (!negotiationId && bookId) {
      const [negRows] = await pool.query(
        `SELECT n.negotiation_id 
         FROM Negotiation n
         JOIN Book b ON n.book_id = b.book_id
         WHERE n.book_id = ? 
           AND (
             (n.buyer_id = ? AND b.seller_id = ?) 
             OR 
             (n.buyer_id = ? AND b.seller_id = ?)
           )
           AND n.status = 'active'
         ORDER BY n.updated_at DESC
         LIMIT 1`,
        [bookId, senderId, receiverId, receiverId, senderId]
      );
      if (negRows.length > 0) {
        negotiationId = negRows[0].negotiation_id;
      }
    }

    const message = await messageService.sendMessage(
      senderId,
      receiverId,
      content,
      negotiationId
    );

    const mappedMessage = {
      _id: String(message.message_id),
      sender: String(message.sender_id),
      content: message.content,
      createdAt: message.created_at
    };

    return res.status(201).json({
      success: true,
      message: 'Message sent successfully',
      conversationId: String(receiverId),
      message: mappedMessage,
      data: {
        message: mappedMessage,
        conversationId: String(receiverId)
      }
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get all conversations for the logged-in user
 * @route GET /api/messages/conversations
 * @access Private
 */
const getConversations = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const conversations = await messageService.getConversations(userId);

    const mappedConvs = conversations.map(c => ({
      _id: String(c.partner_id),
      participants: [
        {
          _id: String(c.partner_id),
          name: c.partner_name,
          profilePicture: c.partner_picture || ''
        },
        {
          _id: String(userId),
          name: 'Me'
        }
      ],
      book: {
        _id: String(c.book_id || ''),
        title: c.book_title || 'Unknown Book',
        price: Number(c.book_asking_price || 0)
      },
      lastMessage: c.last_message ? {
        content: c.last_message,
        createdAt: c.last_message_at
      } : undefined
    }));

    return res.status(200).json({
      success: true,
      message: 'Conversations retrieved successfully',
      conversations: mappedConvs,
      data: mappedConvs
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get message history with a specific user
 * @route GET /api/messages/conversations/:userId
 * @access Private (participant only — enforced by service)
 */
const getConversationHistory = async (req, res, next) => {
  try {
    const currentUserId = req.user.user_id;
    const otherUserId   = req.params.userId || req.params.id;
    const { page, limit } = req.query;

    const result = await messageService.getConversationHistory(
      currentUserId,
      otherUserId,
      page,
      limit
    );

    const mappedMessages = result.messages.map(msg => ({
      _id: String(msg.message_id),
      sender: String(msg.sender_id),
      content: msg.content,
      createdAt: msg.created_at
    }));

    return res.status(200).json({
      success: true,
      message: 'Conversation history retrieved successfully',
      messages: mappedMessages,
      data: mappedMessages
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Mark a message as read
 * @route PUT /api/messages/:id/read
 * @access Private (receiver only)
 */
const markMessageRead = async (req, res, next) => {
  try {
    const messageId = req.params.id;
    const userId    = req.user.user_id;

    const message = await messageService.markMessageRead(messageId, userId);
    return success(res, 'Message marked as read', { message });
  } catch (err) {
    next(err);
  }
};

/**
 * Get unread message count for the logged-in user
 * @route GET /api/messages/unread-count
 * @access Private
 */
const getUnreadCount = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const data   = await messageService.getUnreadCount(userId);
    return success(res, 'Unread count retrieved', data);
  } catch (err) {
    next(err);
  }
};

module.exports = {
  sendMessage,
  getConversations,
  getConversationHistory,
  markMessageRead,
  getUnreadCount,
};
