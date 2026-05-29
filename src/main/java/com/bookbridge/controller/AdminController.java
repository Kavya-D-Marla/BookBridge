package com.bookbridge.controller;

import com.bookbridge.dto.*;
import com.bookbridge.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@Tag(name = "Admin Dashboard APIs", description = "Endpoints for platform administrators to monitor users, textbook listings, transactions, disputes, and analytical parameters (Admin Only)")
public class AdminController {

    private final AdminService adminService;

    @Autowired
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    @Operation(summary = "View platform analytics & statistics", description = "Returns system aggregates including user counts, listings status splits, and successful checkout revenues.")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getStats();
        return ResponseEntity.ok(ApiResponse.success("System analytics retrieved successfully.", stats));
    }

    @GetMapping("/users")
    @Operation(summary = "Retrieve complete users directory", description = "Returns active user directories, showing emails, verification states, and roles.")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users list retrieved.", users));
    }

    @GetMapping("/books")
    @Operation(summary = "Retrieve complete textbooks inventory", description = "Lists complete textbook database records.")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getAllBooks() {
        List<BookResponse> books = adminService.getAllBooks();
        return ResponseEntity.ok(ApiResponse.success("Textbooks inventory list retrieved.", books));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Audit transaction checkout history", description = "Audit platform purchase logs and Razorpay tracking details.")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAllTransactions() {
        List<TransactionResponse> transactions = adminService.getAllTransactions();
        return ResponseEntity.ok(ApiResponse.success("Transactions logs retrieved.", transactions));
    }

    @GetMapping("/disputes")
    @Operation(summary = "View complete active dispute claims queue", description = "Reviews complete dispute database records.")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getAllDisputes() {
        List<DisputeResponse> disputes = adminService.getAllDisputes();
        return ResponseEntity.ok(ApiResponse.success("Dispute claims queue retrieved.", disputes));
    }
}
