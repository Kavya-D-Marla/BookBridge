package com.bookbridge.controller;

import com.bookbridge.dto.ApiResponse;
import com.bookbridge.dto.BookResponse;
import com.bookbridge.security.UserPrincipal;
import com.bookbridge.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@Tag(name = "Wishlist & Watchlist Management", description = "Endpoints for student textbook saves and stock tracking")
public class WishlistController {

    private final WishlistService wishlistService;

    @Autowired
    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @PostMapping("/{bookId}")
    @Operation(summary = "Save a textbook listing to wishlist", description = "Maps the textbook ID to current user. Triggers console observer notification upon restocking.")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long bookId) {
        wishlistService.addBookToWishlist(bookId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Book successfully added to your wishlist."));
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "Remove a saved textbook from wishlist", description = "Unlinks the textbook record from user's watchlist.")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(@AuthenticationPrincipal UserPrincipal principal,
                                                                @PathVariable Long bookId) {
        wishlistService.removeBookFromWishlist(bookId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Book successfully removed from your wishlist."));
    }

    @GetMapping
    @Operation(summary = "View saved textbooks list", description = "Returns all textbook listings saved in the active student's watchlist.")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getWishlist(@AuthenticationPrincipal UserPrincipal principal) {
        List<BookResponse> response = wishlistService.getUserWishlist(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully.", response));
    }
}
