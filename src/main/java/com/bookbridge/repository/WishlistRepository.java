package com.bookbridge.repository;

import com.bookbridge.entity.Book;
import com.bookbridge.entity.User;
import com.bookbridge.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUserAndBook(User user, Book book);
    List<Wishlist> findByUser(User user);
    List<Wishlist> findByBook(Book book);
    boolean existsByUserAndBook(User user, Book book);
    
    @Query("SELECT w.book.bookId, COUNT(w) FROM Wishlist w GROUP BY w.book.bookId")
    List<Object[]> countWishlistsGroupByBook();

    @Query("SELECT w.user FROM Wishlist w WHERE w.book.bookId = :bookId")
    List<User> findUsersByWishedBookId(@Param("bookId") Long bookId);
}
