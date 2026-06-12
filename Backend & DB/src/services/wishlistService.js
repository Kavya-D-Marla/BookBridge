/**
 * Wishlist Service
 * Aligned with frontend schema format
 */

const { pool } = require('../config/db');
const AppError  = require('../utils/AppError');

/**
 * Map wishlist row to frontend WishlistItem structure
 */
const mapWishlistItemToFrontend = (item) => {
  if (!item) return null;
  return {
    _id: String(item.wishlist_id),
    createdAt: item.created_at,
    book: {
      _id: String(item.book_id),
      title: item.title,
      author: item.author,
      condition: item.condition || 'Good',
      price: Number(item.asking_price),
      image: item.image_url || '',
      owner: {
        _id: String(item.seller_id),
        name: item.seller_name || 'Classmate',
      },
    },
  };
};

/**
 * Add a book to the user's wishlist.
 */
const addToWishlist = async (userId, bookId) => {
  const [bookRows] = await pool.query(
    `SELECT book_id, title, author, asking_price, status, image_url, seller_id
     FROM Book WHERE book_id = ? AND status != 'removed'`,
    [bookId]
  );

  if (bookRows.length === 0) {
    throw AppError.notFound('Book not found or no longer available');
  }

  const [existing] = await pool.query(
    'SELECT wishlist_id FROM Wishlist WHERE user_id = ? AND book_id = ?',
    [userId, bookId]
  );

  if (existing.length > 0) {
    throw AppError.conflict('This book is already in your wishlist');
  }

  const [result] = await pool.query(
    'INSERT INTO Wishlist (user_id, book_id) VALUES (?, ?)',
    [userId, bookId]
  );

  const [entryRows] = await pool.query(
    `SELECT
       w.wishlist_id,
       w.user_id,
       w.book_id,
       w.created_at,
       b.title,
       b.author,
       b.asking_price,
       b.status       AS book_status,
       b.image_url,
       b.seller_id,
       b.\`condition\`,
       seller.user_name AS seller_name
     FROM Wishlist w
     JOIN Book b      ON w.book_id   = b.book_id
     JOIN User seller ON b.seller_id = seller.user_id
     WHERE w.wishlist_id = ?`,
    [result.insertId]
  );

  return mapWishlistItemToFrontend(entryRows[0]);
};

/**
 * Get all wishlist entries for a user, with full book details.
 */
const getWishlist = async (userId) => {
  const [rows] = await pool.query(
    `SELECT
       w.wishlist_id,
       w.user_id,
       w.book_id,
       w.created_at,
       b.title,
       b.author,
       b.asking_price,
       b.status       AS book_status,
       b.image_url,
       b.\`condition\`,
       b.semester,
       b.branch,
       b.category,
       b.seller_id,
       seller.user_name       AS seller_name,
       seller.profile_picture AS seller_picture,
       seller.seller_verified AS seller_verified
     FROM Wishlist w
     JOIN Book b      ON w.book_id   = b.book_id
     JOIN User seller ON b.seller_id = seller.user_id
     WHERE w.user_id = ?
     ORDER BY w.created_at DESC`,
    [userId]
  );

  return rows.map(mapWishlistItemToFrontend);
};

/**
 * Remove a book from the user's wishlist.
 */
const removeFromWishlist = async (userId, bookId) => {
  const [existing] = await pool.query(
    'SELECT wishlist_id FROM Wishlist WHERE user_id = ? AND book_id = ?',
    [userId, bookId]
  );

  if (existing.length === 0) {
    throw AppError.notFound('This book is not in your wishlist');
  }

  await pool.query(
    'DELETE FROM Wishlist WHERE user_id = ? AND book_id = ?',
    [userId, bookId]
  );

  return { message: 'Book removed from wishlist' };
};

module.exports = {
  addToWishlist,
  getWishlist,
  removeFromWishlist,
};
