package com.bookbridge.repository;

import com.bookbridge.entity.Book;
import com.bookbridge.entity.BookStatus;
import com.bookbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long>, JpaSpecificationExecutor<Book> {
    List<Book> findBySeller(User seller);
    List<Book> findByStatus(BookStatus status);

    @Query("SELECT b FROM Book b WHERE b.status = 'AVAILABLE'")
    List<Book> findAllAvailable();
}
