package com.bookbridge.service;

import com.bookbridge.dto.BookResponse;
import com.bookbridge.entity.Book;
import com.bookbridge.entity.User;
import com.bookbridge.entity.Wishlist;
import com.bookbridge.exception.BadRequestException;
import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.repository.BookRepository;
import com.bookbridge.repository.UserRepository;
import com.bookbridge.repository.WishlistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Autowired
    public WishlistServiceImpl(WishlistRepository wishlistRepository,
                               BookRepository bookRepository,
                               UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void addBookToWishlist(Long bookId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (wishlistRepository.existsByUserAndBook(user, book)) {
            throw new BadRequestException("This textbook is already in your wishlist!");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .book(book)
                .build();

        wishlistRepository.save(wishlist);
        log.info("Student {} saved book ID: {} to wishlist.", user.getEmail(), bookId);
    }

    @Override
    @Transactional
    public void removeBookFromWishlist(Long bookId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        Wishlist wishlist = wishlistRepository.findByUserAndBook(user, book)
                .orElseThrow(() -> new BadRequestException("This book is not in your wishlist."));

        wishlistRepository.delete(wishlist);
        log.info("Student {} removed book ID: {} from wishlist.", user.getEmail(), bookId);
    }

    @Override
    public List<BookResponse> getUserWishlist(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return wishlistRepository.findByUser(user).stream()
                .map(wishlist -> mapToBookResponse(wishlist.getBook()))
                .collect(Collectors.toList());
    }

    @Override
    public void notifyUsersForBookAvailability(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        // Only notify if book became available
        List<User> usersToNotify = wishlistRepository.findUsersByWishedBookId(bookId);
        
        for (User user : usersToNotify) {
            // Mock Email notification logging to standard output
            log.info("[MOCK NOTIFICATION EMAIL SENT] To: {} | Subject: Textbook Available! " +
                     "| Body: Hello {}, good news! The textbook '{}' which was in your wishlist is now AVAILABLE for {} INR! " +
                     "Hurry up and send an offer to the seller!",
                    user.getEmail(), user.getFirstName(), book.getTitle(), book.getAskingPrice());
        }
    }

    private BookResponse mapToBookResponse(Book book) {
        return BookResponse.builder()
                .bookId(book.getBookId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .edition(book.getEdition())
                .publishedYear(book.getPublishedYear())
                .condition(book.getCondition().name())
                .subject(book.getSubject())
                .description(book.getDescription())
                .askingPrice(book.getAskingPrice())
                .status(book.getStatus().name())
                .sellerId(book.getSeller().getUserId())
                .sellerName(book.getSeller().getFirstName() + " " + book.getSeller().getLastName())
                .imageUrls(book.getImageUrls())
                .createdAt(book.getCreatedAt())
                .build();
    }
}
