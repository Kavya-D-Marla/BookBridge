/**
 * Review Controller
 * Aligned with frontend response formats
 */

const reviewService = require('../services/reviewService');

/**
 * Map review record to frontend structure
 */
const mapReviewToFrontend = (rev) => {
  if (!rev) return null;
  return {
    _id: String(rev.review_id),
    reviewer: {
      name: rev.reviewer_name || 'Classmate',
    },
    rating: Number(rev.rating),
    comment: rev.comment || '',
    createdAt: rev.created_at,
  };
};

/**
 * Create a review for a completed transaction
 */
const createReview = async (req, res, next) => {
  try {
    const reviewerId = req.user.user_id;
    const { transactionId, reviewedUserId, rating, comment } = req.body;

    const review = await reviewService.createReview(
      reviewerId,
      transactionId,
      reviewedUserId,
      rating,
      comment
    );

    const mapped = mapReviewToFrontend(review);

    return res.status(201).json({
      success: true,
      message: 'Review submitted successfully',
      review: mapped,
      data: mapped
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get all reviews for a specific user
 */
const getReviewsForUser = async (req, res, next) => {
  try {
    const userId = req.params.userId || req.params.id;
    const { page, limit } = req.query;

    const result = await reviewService.getReviewsForUser(userId, page, limit);
    const mapped = result.reviews.map(mapReviewToFrontend);

    return res.status(200).json({
      success: true,
      message: 'Reviews retrieved successfully',
      reviews: mapped,
      data: mapped
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get a single review (fallback support)
 */
const getReview = async (req, res, next) => {
  try {
    const reviewId = req.params.id;
    const review   = await reviewService.getReview(reviewId);
    const mapped = mapReviewToFrontend(review);
    
    return res.status(200).json({
      success: true,
      message: 'Review retrieved successfully',
      review: mapped,
      data: mapped
    });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  createReview,
  getReviewsForUser,
  getReview,
};
