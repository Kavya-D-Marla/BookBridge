package com.bookbridge.controller;

import com.bookbridge.dto.BookCreateRequest;
import com.bookbridge.dto.BookResponse;
import com.bookbridge.dto.ApiResponse;
import com.bookbridge.security.UserPrincipal;
import com.bookbridge.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Book Listing & Search System", description = "Endpoints for textbook list CRUD, custom relevance search sorting, and price recommendation algorithm")
public class BookController {

    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @Operation(summary = "List a new secondhand textbook for sale", description = "Creates a new textbook entry with price, author, and description.")
    public ResponseEntity<ApiResponse<BookResponse>> addBook(@AuthenticationPrincipal UserPrincipal principal,
                                                             @Valid @RequestBody BookCreateRequest request) {
        BookResponse response = bookService.addBook(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Textbook listed successfully.", response));
    }

    @PutMapping("/{bookId}")
    @Operation(summary = "Update an existing textbook listing details", description = "Edits specific textbook attributes. Only the listing seller can edit.")
    public ResponseEntity<ApiResponse<BookResponse>> updateBook(@AuthenticationPrincipal UserPrincipal principal,
                                                                @PathVariable Long bookId,
                                                                @Valid @RequestBody BookCreateRequest request) {
        BookResponse response = bookService.updateBook(bookId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Listing updated successfully.", response));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "Delete an active textbook listing", description = "Removes the textbook listing. Authorized to the seller or administrators.")
    public ResponseEntity<ApiResponse<Void>> deleteBook(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long bookId) {
        bookService.deleteBook(bookId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Textbook listing deleted successfully."));
    }

    @GetMapping("/details/{bookId}")
    @Operation(summary = "Fetch textbook detailed information", description = "Returns full textbook parameters and increments click views counter.")
    public ResponseEntity<ApiResponse<BookResponse>> getBookDetails(@PathVariable Long bookId) {
        BookResponse response = bookService.getBookDetails(bookId);
        return ResponseEntity.ok(ApiResponse.success("Book details retrieved.", response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search, filter and sort textbooks dynamically", description = "Uses a custom relevance search algorithm ranking matches by titles/authors, filtering by subjects/prices/conditions, and sorting dynamically.")
    public ResponseEntity<ApiResponse<List<BookResponse>>> searchBooks(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<BookResponse> response = bookService.searchAndFilterBooks(query, subject, minPrice, maxPrice, condition, sortBy, page, size);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved.", response));
    }

    @GetMapping("/recommend-price")
    @Operation(summary = "Estimate a fair textbook resale value", description = "Uses the Price Recommendation Algorithm incorporating quality condition grading and publication year to suggest a market price.")
    public ResponseEntity<ApiResponse<BigDecimal>> getPriceRecommendation(
            @RequestParam BigDecimal originalMSRP,
            @RequestParam String condition,
            @RequestParam int publishedYear) {
        BigDecimal recommendedPrice = bookService.getRecommendedPrice(originalMSRP, condition, publishedYear);
        return ResponseEntity.ok(ApiResponse.success("Suggested fair market price calculated successfully.", recommendedPrice));
    }

    @GetMapping("/popular")
    @Operation(summary = "Rank most popular textbooks in the ecosystem", description = "Uses the Popular Books Algorithm, calculating a popularity index using clicks and wishlist counts.")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getPopularBooks() {
        List<BookResponse> response = bookService.getPopularBooks();
        return ResponseEntity.ok(ApiResponse.success("Top popular textbook listings retrieved.", response));
    }

    @GetMapping("/my-listings")
    @Operation(summary = "Retrieve active user listings", description = "Lists textbooks offered for sale by the authenticated user.")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getMyListings(@AuthenticationPrincipal UserPrincipal principal) {
        List<BookResponse> response = bookService.getMyListings(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Your textbook listings retrieved.", response));
    }
}
