package com.bookbridge.controller;

import com.bookbridge.dto.ApiResponse;
import com.bookbridge.dto.PaymentOrderResponse;
import com.bookbridge.dto.PaymentVerificationRequest;
import com.bookbridge.dto.TransactionResponse;
import com.bookbridge.security.UserPrincipal;
import com.bookbridge.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction Management", description = "Endpoints for Razorpay order generation, signature checks, transaction receipt downloads, and history checks")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/checkout/{negotiationId}")
    @Operation(summary = "Initialize textbook checkout order", description = "Constructs a Razorpay payment order for agreed negotiation sum. Saves a PENDING transaction log.")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> checkout(@AuthenticationPrincipal UserPrincipal principal,
                                                                      @PathVariable Long negotiationId) {
        PaymentOrderResponse response = transactionService.initiatePayment(negotiationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Checkout order initialized successfully.", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature", description = "Checks signature validity. Upon success, changes payment state to SUCCESS, tags book as SOLD, and auto-expires other open listings negotiations.")
    public ResponseEntity<ApiResponse<TransactionResponse>> verifyPayment(@AuthenticationPrincipal UserPrincipal principal,
                                                                          @Valid @RequestBody PaymentVerificationRequest request) {
        TransactionResponse response = transactionService.verifyPaymentAndCompleteTransaction(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "SUCCESS".equals(response.getPaymentStatus()) ? "Payment verified. Transaction completed successfully!" : "Payment signature check failed. Transaction aborted.",
                response
        ));
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "View transaction log details", description = "Returns single purchase summary. Accessible to transaction buyer, seller, or platform admin.")
    public ResponseEntity<ApiResponse<TransactionResponse>> getDetails(@AuthenticationPrincipal UserPrincipal principal,
                                                                       @PathVariable Long transactionId) {
        TransactionResponse response = transactionService.getTransactionDetails(transactionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Transaction log details retrieved.", response));
    }

    @GetMapping
    @Operation(summary = "View transaction history", description = "Lists student checkout purchases and sold history listings.")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getHistory(@AuthenticationPrincipal UserPrincipal principal) {
        List<TransactionResponse> response = transactionService.getMyTransactions(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved successfully.", response));
    }

    @GetMapping("/{transactionId}/receipt")
    @Operation(summary = "Generate textbook purchase receipt", description = "Returns a formatted textual payment voucher sheet for successful transactions.")
    public ResponseEntity<ApiResponse<String>> downloadReceipt(@AuthenticationPrincipal UserPrincipal principal,
                                                               @PathVariable Long transactionId) {
        String receipt = transactionService.downloadReceipt(transactionId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Voucher receipt sheet generated.", receipt));
    }
}
