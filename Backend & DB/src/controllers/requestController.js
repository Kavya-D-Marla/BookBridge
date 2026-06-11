/**
 * Request Controller
 * Redirected to handle Book Trade Negotiations for frontend alignment
 */

const { pool } = require('../config/db');
const negotiationService = require('../services/negotiationService');

/**
 * Map negotiation database record to frontend request structure
 */
const mapNegotiationToRequest = (neg) => {
  if (!neg) return null;

  let frontendStatus = 'Pending';
  if (neg.status === 'accepted') {
    frontendStatus = 'Accepted';
  } else if (neg.status === 'rejected' || neg.status === 'cancelled' || neg.status === 'expired') {
    frontendStatus = 'Declined';
  } else if (neg.status === 'active') {
    frontendStatus = neg.offer_count > 1 ? 'Negotiating' : 'Pending';
  }

  return {
    _id: String(neg.negotiation_id),
    book: {
      _id: String(neg.book_id),
      title: neg.book_title,
      author: neg.book_author || '',
      price: Number(neg.book_asking_price),
      type: neg.book_type || 'Sell',
      image: neg.book_image_url || '',
    },
    buyer: {
      _id: String(neg.buyer_id),
      name: neg.buyer_name || 'Classmate',
      rating: 4.8,
    },
    seller: {
      _id: String(neg.seller_id),
      name: neg.seller_name || 'Classmate',
      rating: 4.8,
    },
    status: frontendStatus,
    proposedPrice: neg.latest_offer_price ? Number(neg.latest_offer_price) : undefined,
    offeredBook: neg.offered_book_id ? {
      _id: String(neg.offered_book_id),
      title: neg.offered_book_title || 'Swap book',
      author: neg.offered_book_author || '',
      price: 0,
      type: 'Exchange'
    } : undefined,
    message: neg.initial_message || '',
    createdAt: neg.created_at,
  };
};

/**
 * Get all book negotiations involving current user
 */
const getRequests = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const role = req.user.role;
    const negotiations = await negotiationService.getUserNegotiations(userId, role);
    const mapped = negotiations.map(mapNegotiationToRequest);
    
    return res.status(200).json({
      success: true,
      message: 'Negotiations retrieved successfully',
      requests: mapped,
      data: mapped
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Start a new negotiation
 */
const createRequest = async (req, res, next) => {
  try {
    const buyerId = req.user.user_id;
    const { bookId, message, proposedPrice, offeredBookId } = req.body;
    
    let finalPrice = proposedPrice;
    if (finalPrice === undefined || finalPrice === null) {
      const [books] = await pool.query('SELECT asking_price FROM Book WHERE book_id = ?', [bookId]);
      finalPrice = books.length > 0 ? books[0].asking_price : 0;
    }

    const result = await negotiationService.createNegotiation(
      buyerId, 
      parseInt(bookId, 10), 
      parseFloat(finalPrice), 
      message, 
      offeredBookId ? parseInt(offeredBookId, 10) : null
    );

    const mapped = mapNegotiationToRequest(result.negotiation);
    
    return res.status(201).json({
      success: true,
      message: 'Negotiation started successfully',
      request: mapped,
      data: mapped
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Accept or decline a trade negotiation
 */
const updateRequestStatus = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const negotiationId = req.params.id;
    const { status } = req.body; // 'Accepted' or 'Declined'

    let result;
    if (status === 'Accepted') {
      result = await negotiationService.acceptOffer(negotiationId, userId);
    } else {
      result = await negotiationService.rejectNegotiation(negotiationId, userId);
    }

    const mapped = mapNegotiationToRequest(result.negotiation || result);
    
    return res.status(200).json({
      success: true,
      message: 'Negotiation status updated successfully',
      request: mapped,
      data: mapped
    });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  getRequests,
  createRequest,
  updateRequestStatus,
};
