/**
 * Request Routes (Aligned with frontend Book Trade Negotiations)
 * Base path: /api/requests
 */

const express = require('express');
const requestController = require('../controllers/requestController');
const { verifyJWT } = require('../middleware/auth');
const { validateIdParam, handleValidationErrors } = require('../middleware/validate');

const router = express.Router();

// All request routes require authentication
router.use(verifyJWT);

/**
 * @route   GET /api/requests
 * @desc    Get all trade negotiations for current user
 */
router.get('/', requestController.getRequests);

/**
 * @route   POST /api/requests
 * @desc    Start a new book trade negotiation
 */
router.post('/', requestController.createRequest);

/**
 * @route   PATCH /api/requests/:id/status
 * @desc    Accept or Decline a negotiation
 */
router.patch(
  '/:id/status',
  validateIdParam('id'),
  handleValidationErrors,
  requestController.updateRequestStatus
);

module.exports = router;
