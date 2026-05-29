package com.bookbridge.algorithms;

import com.bookbridge.entity.Book;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PopularBooks {

    /**
     * Ranks books based on their popularity.
     * Popularity is calculated using a weighted score of Views and Wishlist additions.
     * 
     * Score Formula: Popularity = (ViewsCount * 1.0) + (WishlistCount * 3.0)
     * Wishlists are weighted 3x higher since saving reflects a stronger purchase intent.
     *
     * @param books               List of candidate books.
     * @param bookWishlistCounts Map containing book IDs associated with their total wishlist count.
     * @return Sorted list of books from most popular to least popular.
     */
    public List<Book> rankPopularBooks(List<Book> books, Map<Long, Long> bookWishlistCounts) {
        if (books == null || books.isEmpty()) {
            return new ArrayList<>();
        }

        return books.stream()
            .map(book -> {
                long wishlistCount = bookWishlistCounts != null ? bookWishlistCounts.getOrDefault(book.getBookId(), 0L) : 0L;
                double score = (book.getViewsCount() * 1.0) + (wishlistCount * 3.0);
                return new BookPopularityPair(book, score);
            })
            .sorted((pair1, pair2) -> Double.compare(pair2.score, pair1.score)) // Descending order of popularity
            .map(pair -> pair.book)
            .collect(Collectors.toList());
    }

    private static class BookPopularityPair {
        final Book book;
        final double score;

        BookPopularityPair(Book book, double score) {
            this.book = book;
            this.score = score;
        }
    }
}
