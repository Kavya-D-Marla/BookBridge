package com.bookbridge.service;

import com.bookbridge.algorithms.PopularBooks;
import com.bookbridge.algorithms.PriceRecommendation;
import com.bookbridge.algorithms.SearchRanking;
import com.bookbridge.dto.BookCreateRequest;
import com.bookbridge.dto.BookResponse;
import com.bookbridge.entity.Book;
import com.bookbridge.entity.BookCondition;
import com.bookbridge.entity.BookStatus;
import com.bookbridge.entity.User;
import com.bookbridge.exception.BadRequestException;
import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.exception.UnauthorizedException;
import com.bookbridge.repository.BookRepository;
import com.bookbridge.repository.UserRepository;
import com.bookbridge.repository.WishlistRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final WishlistRepository wishlistRepository;
    private final SearchRanking searchRankingAlgorithm;
    private final PriceRecommendation priceRecommendationAlgorithm;
    private final PopularBooks popularBooksAlgorithm;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository,
                           UserRepository userRepository,
                           WishlistRepository wishlistRepository,
                           SearchRanking searchRankingAlgorithm,
                           PriceRecommendation priceRecommendationAlgorithm,
                           PopularBooks popularBooksAlgorithm) {
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.wishlistRepository = wishlistRepository;
        this.searchRankingAlgorithm = searchRankingAlgorithm;
        this.priceRecommendationAlgorithm = priceRecommendationAlgorithm;
        this.popularBooksAlgorithm = popularBooksAlgorithm;
    }

    @Override
    @Transactional
    public BookResponse addBook(BookCreateRequest request, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + sellerId));

        BookCondition condition;
        try {
            condition = BookCondition.valueOf(request.getCondition().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid book condition grading: " + request.getCondition());
        }

        Book book = Book.builder()
                .title(request.getTitle().trim())
                .author(request.getAuthor().trim())
                .edition(request.getEdition())
                .publishedYear(request.getPublishedYear())
                .condition(condition)
                .subject(request.getSubject() != null ? request.getSubject().trim() : null)
                .description(request.getDescription())
                .askingPrice(request.getAskingPrice())
                .status(BookStatus.AVAILABLE)
                .seller(seller)
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>())
                .viewsCount(0)
                .build();

        Book savedBook = bookRepository.save(book);
        log.info("Successfully listed textbook: {} by seller: {}", savedBook.getTitle(), seller.getEmail());
        return mapToBookResponse(savedBook);
    }

    @Override
    @Transactional
    public BookResponse updateBook(Long bookId, BookCreateRequest request, Long currentUserId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        if (!book.getSeller().getUserId().equals(currentUserId)) {
            throw new UnauthorizedException("You are not allowed to update someone else's textbook listing.");
        }

        BookCondition condition;
        try {
            condition = BookCondition.valueOf(request.getCondition().toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid book condition grading: " + request.getCondition());
        }

        book.setTitle(request.getTitle().trim());
        book.setAuthor(request.getAuthor().trim());
        book.setEdition(request.getEdition());
        book.setPublishedYear(request.getPublishedYear());
        book.setCondition(condition);
        book.setSubject(request.getSubject() != null ? request.getSubject().trim() : null);
        book.setDescription(request.getDescription());
        book.setAskingPrice(request.getAskingPrice());
        if (request.getImageUrls() != null) {
            book.setImageUrls(request.getImageUrls());
        }

        Book updatedBook = bookRepository.save(book);
        log.info("Updated textbook listing ID: {}", updatedBook.getBookId());
        return mapToBookResponse(updatedBook);
    }

    @Override
    @Transactional
    public void deleteBook(Long bookId, Long currentUserId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only seller or administrator can delete
        if (!book.getSeller().getUserId().equals(currentUserId) && !user.getRole().name().equals("ROLE_ADMIN")) {
            throw new UnauthorizedException("You are not authorized to delete this textbook listing.");
        }

        bookRepository.delete(book);
        log.info("Deleted book listing ID: {}", bookId);
    }

    @Override
    @Transactional
    public BookResponse getBookDetails(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        // Increment Views for popularity metrics
        book.setViewsCount(book.getViewsCount() + 1);
        bookRepository.save(book);

        return mapToBookResponse(book);
    }

    @Override
    public List<BookResponse> searchAndFilterBooks(String query, String subject, 
                                                   BigDecimal minPrice, BigDecimal maxPrice, 
                                                   String condition, String sortBy, 
                                                   int page, int size) {
        
        Specification<Book> spec = Specification.where(null);

        if (subject != null && !subject.trim().isEmpty()) {
            spec = spec.and((root, q, cb) -> cb.equal(cb.lower(root.get("subject")), subject.toLowerCase().trim()));
        }

        if (minPrice != null) {
            spec = spec.and((root, q, cb) -> cb.ge(root.get("askingPrice"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, q, cb) -> cb.le(root.get("askingPrice"), maxPrice));
        }

        if (condition != null && !condition.trim().isEmpty()) {
            try {
                BookCondition bc = BookCondition.valueOf(condition.toUpperCase().replace(" ", "_"));
                spec = spec.and((root, q, cb) -> cb.equal(root.get("condition"), bc));
            } catch (IllegalArgumentException e) {
                // Ignore invalid condition query or handle gracefully
            }
        }

        // Fetch matches
        List<Book> matchedBooks = bookRepository.findAll(spec);

        // Rank the retrieved lists using our Custom Search Ranking Algorithm!
        List<Book> rankedBooks = searchRankingAlgorithm.rankBooks(matchedBooks, query);

        // Sorting hooks (e.g. price)
        if ("price_asc".equalsIgnoreCase(sortBy)) {
            rankedBooks.sort(Comparator.comparing(Book::getAskingPrice));
        } else if ("price_desc".equalsIgnoreCase(sortBy)) {
            rankedBooks.sort((b1, b2) -> b2.getAskingPrice().compareTo(b1.getAskingPrice()));
        } else if ("date_asc".equalsIgnoreCase(sortBy)) {
            rankedBooks.sort(Comparator.comparing(Book::getCreatedAt));
        }

        // Apply Manual Pagination safely on our Custom Sorted Collection
        int fromIndex = page * size;
        if (fromIndex >= rankedBooks.size()) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + size, rankedBooks.size());

        List<Book> paginatedBooks = rankedBooks.subList(fromIndex, toIndex);
        return paginatedBooks.stream().map(this::mapToBookResponse).collect(Collectors.toList());
    }

    @Override
    public BigDecimal getRecommendedPrice(BigDecimal originalMSRP, String condition, int publishedYear) {
        BookCondition bc;
        try {
            bc = BookCondition.valueOf(condition.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid book condition grading: " + condition);
        }
        return priceRecommendationAlgorithm.suggestPrice(originalMSRP, bc, publishedYear);
    }

    @Override
    public List<BookResponse> getPopularBooks() {
        List<Book> allBooks = bookRepository.findAllAvailable();
        
        // Count wishlists grouped by Book ID
        List<Object[]> wishlistResults = wishlistRepository.countWishlistsGroupByBook();
        Map<Long, Long> bookWishlistCounts = new HashMap<>();
        for (Object[] row : wishlistResults) {
            bookWishlistCounts.put((Long) row[0], (Long) row[1]);
        }

        // Use custom Popular Books Algorithm to calculate scores
        List<Book> rankedPopular = popularBooksAlgorithm.rankPopularBooks(allBooks, bookWishlistCounts);

        // Return Top 10 popular books
        return rankedPopular.stream()
                .limit(10)
                .map(this::mapToBookResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookResponse> getMyListings(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + sellerId));
        return bookRepository.findBySeller(seller).stream()
                .map(this::mapToBookResponse)
                .collect(Collectors.toList());
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
                .viewsCount(book.getViewsCount())
                .createdAt(book.getCreatedAt())
                .build();
    }
}
