/**
 * Book Controller
 * Handles HTTP requests for book endpoints
 */

const bookService = require('../services/bookService');
const { success } = require('../utils/responseHelper');

/**
 * Get all books (with search, filter, pagination)
 * @route GET /api/books
 * @access Public
 */
const getBooks = async (req, res, next) => {
  try {
    const filters = req.query;
    const result = await bookService.getBooks(filters);
    
    return res.status(200).json({
      success: true,
      message: 'Books retrieved successfully',
      books: result.books,
      pagination: result.pagination,
      data: result.books
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get a single book by ID
 * @route GET /api/books/:id
 * @access Public
 */
const getBookById = async (req, res, next) => {
  try {
    const bookId = req.params.id;
    const book = await bookService.getBookById(bookId);
    
    return res.status(200).json({
      success: true,
      message: 'Book retrieved successfully',
      ...book,
      data: book
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Get user's own listings (inventory)
 * @route GET /api/books/my-inventory
 * @access Private (Verified Seller)
 */
const getMyInventory = async (req, res, next) => {
  try {
    const userId = req.user.user_id;
    const books = await bookService.getMyInventory(userId);
    
    return res.status(200).json({
      success: true,
      message: 'My inventory retrieved successfully',
      books,
      data: books
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Create a new book listing
 * @route POST /api/books
 * @access Private (Verified Seller)
 */
const createBook = async (req, res, next) => {
  try {
    const sellerId = req.user.user_id;
    const bookData = req.body;
    
    const newBook = await bookService.createBook(sellerId, bookData);
    
    return res.status(201).json({
      success: true,
      message: 'Book listing created successfully',
      ...newBook,
      data: newBook
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Update a book listing
 * @route PUT /api/books/:id
 * @access Private (Verified Seller - Owner or Admin)
 */
const updateBook = async (req, res, next) => {
  try {
    const bookId = req.params.id;
    const userId = req.user.user_id;
    const userRole = req.user.role;
    const updateData = req.body;

    const updatedBook = await bookService.updateBook(bookId, userId, updateData, userRole);
    
    return res.status(200).json({
      success: true,
      message: 'Book listing updated successfully',
      ...updatedBook,
      data: updatedBook
    });
  } catch (err) {
    next(err);
  }
};

/**
 * Delete a book listing (soft delete)
 * @route DELETE /api/books/:id
 * @access Private (Verified Seller - Owner or Admin)
 */
const deleteBook = async (req, res, next) => {
  try {
    const bookId = req.params.id;
    const userId = req.user.user_id;
    const userRole = req.user.role;

    const result = await bookService.deleteBook(bookId, userId, userRole);
    return success(res, result.message);
  } catch (err) {
    next(err);
  }
};

/**
 * Get price recommendation based on condition
 * @route GET /api/books/price-recommendation
 * @access Private (Verified Seller)
 */
const getPriceRecommendation = (req, res, next) => {
  try {
    const { originalPrice, condition } = req.query;
    
    if (!originalPrice || !condition) {
      return res.status(400).json({
        success: false,
        message: 'Original price and condition are required',
      });
    }

    const recommendation = bookService.getPriceRecommendation({
      originalPrice: parseFloat(originalPrice),
      condition
    });

    return success(res, 'Price recommendation generated', recommendation);
  } catch (err) {
    next(err);
  }
};

module.exports = {
  getBooks,
  getBookById,
  getMyInventory,
  createBook,
  updateBook,
  deleteBook,
  getPriceRecommendation,
};
