/**
 * Negotiation Controller
 * Aligned with frontend Book Trade Negotiations
 */

const negotiationService = require('../services/negotiationService');
const { success } = require('../utils/responseHelper');
const AppError = require('../utils/AppError');

/**
 * Handle new negotiation OR counteroffer
 * @route POST /api/negotiations
 */
const createNegotiation = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const { bookId, offeredPrice, requestId, message, proposedPrice } = req.body;

    if (requestId) {
      // Counter-offer sent from Requests panel
      if (proposedPrice === undefined || proposedPrice === null) {
        throw AppError.badRequest('proposedPrice is required for a counteroffer');
      }

      const offer = await negotiationService.createOffer(
        parseInt(requestId, 10),
        userId,
        parseFloat(proposedPrice),
        message
      );
      
      return success(res, 'Counteroffer submitted successfully', offer, 201);
    } else {
      // Starting a new negotiation from BookDetails page
      if (!bookId || offeredPrice === undefined) {
        throw AppError.badRequest('bookId and offeredPrice are required');
      }

      const result = await negotiationService.createNegotiation(
        userId,
        parseInt(bookId, 10),
        parseFloat(offeredPrice)
      );

      return success(res, 'Negotiation started successfully', result, 201);
    }
  } catch (err) {
    next(err);
  }
};

/**
 * Get user negotiations
 */
const getUserNegotiations = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const role   = req.user.role;
    const negotiations = await negotiationService.getUserNegotiations(userId, role);
    return success(res, 'Negotiations retrieved successfully', { negotiations });
  } catch (err) {
    next(err);
  }
};

/**
 * Get negotiation history directly (mapped for Requests.tsx)
 * @route GET /api/negotiations/:id
 */
const getNegotiationDetails = async (req, res, next) => {
  try {
    const negotiationId = req.params.id;
    const userId        = req.user.user_id;
    const role          = req.user.role;

    const result = await negotiationService.getNegotiationHistory(
      negotiationId,
      userId,
      role
    );

    // Map history to frontend rounds format
    const mappedHistory = result.history.map(row => ({
      _id: String(row.offer_id),
      sender: {
        _id: String(row.user_id),
        name: row.user_name || 'Classmate',
      },
      message: row.message || '',
      proposedPrice: Number(row.offered_price),
      createdAt: row.timestamp,
    }));

    return success(res, 'Negotiation history retrieved successfully', mappedHistory);
  } catch (err) {
    next(err);
  }
};

/**
 * Counteroffer (legacy support)
 */
const createOffer = async (req, res, next) => {
  try {
    const negotiationId = req.params.id;
    const userId        = req.user.user_id;
    const { offeredPrice, message } = req.body;

    const offer = await negotiationService.createOffer(
      negotiationId,
      userId,
      offeredPrice,
      message
    );

    return success(res, 'Offer submitted successfully', { offer }, 201);
  } catch (err) {
    next(err);
  }
};

/**
 * Accept offer
 */
const acceptOffer = async (req, res, next) => {
  try {
    const negotiationId = req.params.id;
    const userId        = req.user.user_id;
    const result = await negotiationService.acceptOffer(negotiationId, userId);
    return success(res, 'Offer accepted. Agreement recorded.', result);
  } catch (err) {
    next(err);
  }
};

/**
 * Reject negotiation
 */
const rejectNegotiation = async (req, res, next) => {
  try {
    const negotiationId = req.params.id;
    const userId        = req.user.user_id;
    const negotiation = await negotiationService.rejectNegotiation(negotiationId, userId);
    return success(res, 'Negotiation rejected', { negotiation });
  } catch (err) {
    next(err);
  }
};

/**
 * Cancel negotiation
 */
const cancelNegotiation = async (req, res, next) => {
  try {
    const negotiationId = req.params.id;
    const userId        = req.user.user_id;
    const negotiation = await negotiationService.cancelNegotiation(negotiationId, userId);
    return success(res, 'Negotiation cancelled', { negotiation });
  } catch (err) {
    next(err);
  }
};

/**
 * Get history (legacy support)
 */
const getNegotiationHistory = async (req, res, next) => {
  try {
    const negotiationId = req.params.id;
    const userId        = req.user.user_id;
    const role          = req.user.role;
    const result = await negotiationService.getNegotiationHistory(negotiationId, userId, role);
    return success(res, 'Negotiation history retrieved successfully', result);
  } catch (err) {
    next(err);
  }
};

module.exports = {
  createNegotiation,
  getUserNegotiations,
  getNegotiationDetails,
  createOffer,
  acceptOffer,
  rejectNegotiation,
  cancelNegotiation,
  getNegotiationHistory,
};
