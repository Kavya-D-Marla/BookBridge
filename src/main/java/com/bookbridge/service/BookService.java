package com.bookbridge.service;

import com.bookbridge.dto.BookCreateRequest;
import com.bookbridge.dto.BookResponse;

import java.math.BigDecimal;
import java.util.List;

public interface BookService {
    BookResponse addBook(BookCreateRequest request, Long sellerId);
    BookResponse updateBook(Long bookId, BookCreateRequest request, Long currentUserId);
    void deleteBook(Long bookId, Long currentUserId);
    BookResponse getBookDetails(Long bookId);
    
    List<BookResponse> searchAndFilterBooks(String query, String subject, 
                                           BigDecimal minPrice, BigDecimal maxPrice, 
                                           String condition, String sortBy, 
                                           int page, int size);
                                           
    BigDecimal getRecommendedPrice(BigDecimal originalMSRP, String condition, int publishedYear);
    List<BookResponse> getPopularBooks();
    List<BookResponse> getMyListings(Long sellerId);
}
