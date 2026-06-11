const express = require('express');
const { verifyJWT } = require('../middleware/auth');
const { pool } = require('../config/db');
const { success } = require('../utils/responseHelper');
const AppError = require('../utils/AppError');

const router = express.Router();

router.use(verifyJWT);

/**
 * @route   PUT /api/users/profile
 * @desc    Update user profile details
 * @access  Private
 */
router.put('/profile', async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const { name, profilePicture } = req.body;

    if (!name) {
      throw AppError.badRequest('Name is required');
    }

    await pool.query(
      `UPDATE User 
       SET user_name = ?, profile_picture = ?, updated_at = CURRENT_TIMESTAMP
       WHERE user_id = ?`,
      [name, profilePicture || null, userId]
    );

    const [rows] = await pool.query(
      'SELECT user_id, google_id, user_name, email, email_verified, profile_picture, role, seller_verified, usn FROM User WHERE user_id = ?',
      [userId]
    );

    const updatedUser = rows[0];

    const mappedUser = {
      id: String(updatedUser.user_id),
      name: updatedUser.user_name,
      email: updatedUser.email,
      role: updatedUser.role,
      profilePicture: updatedUser.profile_picture,
      sellerVerified: !!updatedUser.seller_verified,
    };

    return success(res, 'Profile updated successfully', mappedUser);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
