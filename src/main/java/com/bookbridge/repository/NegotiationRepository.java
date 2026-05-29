package com.bookbridge.repository;

import com.bookbridge.entity.Book;
import com.bookbridge.entity.Negotiation;
import com.bookbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NegotiationRepository extends JpaRepository<Negotiation, Long> {
    List<Negotiation> findByBuyer(User buyer);
    List<Negotiation> findBySeller(User seller);
    Optional<Negotiation> findByBookAndBuyer(Book book, User buyer);
    List<Negotiation> findByBook(Book book);
}
