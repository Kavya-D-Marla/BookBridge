/**
 * Wishlist Controller
 * Aligned with frontend response formats
 */

const wishlistService = require('../services/wishlistService');

/**
 * Add a book to the wishlist
 */
const addToWishlist = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const { bookId } = req.body;

    const entry = await wishlistService.addToWishlist(userId, bookId);
    
    return res.status(201).json({
      success: true,
      message: 'Book added to wishlist',
      entry,
      data: entry
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get current user's full wishlist
 */
const getWishlist = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const wishlist = await wishlistService.getWishlist(userId);
    
    return res.status(200).json({
      success: true,
      message: 'Wishlist retrieved successfully',
      wishlist,
      data: wishlist
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Remove a book from the wishlist
 */
const removeFromWishlist = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const bookId = req.params.bookId;

    const result = await wishlistService.removeFromWishlist(userId, bookId);
    
    return res.status(200).json({
      success: true,
      message: result.message
    });
  } catch (err) {
    next(err);
  }
};

module.exports = {
  addToWishlist,
  getWishlist,
  removeFromWishlist,
};
