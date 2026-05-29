package com.bookbridge.algorithms;

import com.bookbridge.entity.Book;
import com.bookbridge.entity.BookStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SearchRanking {

    /**
     * Ranks books based on a relevance scoring algorithm.
     * 
     * @param books List of raw books matching basic filters.
     * @param query The search query string input by the user.
     * @return Sorted list of books in descending order of relevance.
     */
    public List<Book> rankBooks(List<Book> books, String query) {
        if (books == null || books.isEmpty()) {
            return new ArrayList<>();
        }

        if (query == null || query.trim().isEmpty()) {
            // If no search text query, sort by status (Available first) and then creation date (newest first)
            return books.stream()
                .sorted((b1, b2) -> {
                    int statusCompare = compareStatus(b1.getStatus(), b2.getStatus());
                    if (statusCompare != 0) return statusCompare;
                    return b2.getCreatedAt().compareTo(b1.getCreatedAt());
                })
                .collect(Collectors.toList());
        }

        final String cleanQuery = query.toLowerCase().trim();
        final String[] queryKeywords = cleanQuery.split("\\s+");

        return books.stream()
            .map(book -> new BookScorePair(book, calculateRelevanceScore(book, cleanQuery, queryKeywords)))
            .sorted((pair1, pair2) -> Double.compare(pair2.score, pair1.score)) // Descending score
            .map(pair -> pair.book)
            .collect(Collectors.toList());
    }

    private double calculateRelevanceScore(Book book, String cleanQuery, String[] keywords) {
        double score = 0.0;

        String title = book.getTitle() != null ? book.getTitle().toLowerCase() : "";
        String author = book.getAuthor() != null ? book.getAuthor().toLowerCase() : "";
        String subject = book.getSubject() != null ? book.getSubject().toLowerCase() : "";
        String description = book.getDescription() != null ? book.getDescription().toLowerCase() : "";

        // 1. Direct Whole String Matches
        if (title.equals(cleanQuery)) {
            score += 200.0;
        } else if (title.contains(cleanQuery)) {
            score += 100.0;
        }

        if (author.equals(cleanQuery)) {
            score += 120.0;
        } else if (author.contains(cleanQuery)) {
            score += 60.0;
        }

        // 2. Keyword Keyword Matches
        for (String keyword : keywords) {
            if (keyword.isEmpty()) continue;

            if (title.contains(keyword)) {
                score += 40.0;
            }
            if (author.contains(keyword)) {
                score += 25.0;
            }
            if (subject.contains(keyword)) {
                score += 20.0;
            }
            if (description.contains(keyword)) {
                score += 5.0;
            }
        }

        // 3. Status Boost (Available books are ranked higher than Sold/Reserved)
        if (book.getStatus() == BookStatus.AVAILABLE) {
            score += 50.0;
        } else if (book.getStatus() == BookStatus.RESERVED) {
            score += 10.0;
        }

        // 4. Condition Boost (Nicer condition books get higher default placement)
        score += switch (book.getCondition()) {
            case NEW -> 15.0;
            case LIKE_NEW -> 10.0;
            case GOOD -> 5.0;
            case ACCEPTABLE -> 1.0;
        };

        // 5. Recency / Year Boost
        if (book.getPublishedYear() != null) {
            // Small boost for books published after 2020
            score += Math.max(0, (book.getPublishedYear() - 2010) * 0.5);
        }

        return score;
    }

    private int compareStatus(BookStatus s1, BookStatus s2) {
        if (s1 == s2) return 0;
        if (s1 == BookStatus.AVAILABLE) return -1;
        if (s2 == BookStatus.AVAILABLE) return 1;
        if (s1 == BookStatus.RESERVED) return -1;
        if (s2 == BookStatus.RESERVED) return 1;
        return 0;
    }

    private static class BookScorePair {
        final Book book;
        final double score;

        BookScorePair(Book book, double score) {
            this.book = book;
            this.score = score;
        }
    }
}
