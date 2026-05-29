package com.bookbridge.service;

import com.bookbridge.dto.BookResponse;

import java.util.List;

public interface WishlistService {
    void addBookToWishlist(Long bookId, Long userId);
    void removeBookFromWishlist(Long bookId, Long userId);
    List<BookResponse> getUserWishlist(Long userId);
    void notifyUsersForBookAvailability(Long bookId);
}
