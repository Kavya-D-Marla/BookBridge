package com.bookbridge.service;

import com.bookbridge.dto.AdminStatsResponse;
import com.bookbridge.dto.BookResponse;
import com.bookbridge.dto.DisputeResponse;
import com.bookbridge.dto.TransactionResponse;
import com.bookbridge.dto.UserResponse;

import java.util.List;

public interface AdminService {
    AdminStatsResponse getStats();
    List<UserResponse> getAllUsers();
    List<BookResponse> getAllBooks();
    List<TransactionResponse> getAllTransactions();
    List<DisputeResponse> getAllDisputes();
}
