package com.bookbridge.repository;

import com.bookbridge.entity.Transaction;
import com.bookbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByBuyer(User buyer);
    List<Transaction> findBySeller(User seller);
    Optional<Transaction> findByRazorpayOrderId(String razorpayOrderId);
}
