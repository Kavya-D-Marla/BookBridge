package com.bookbridge.controller;

import com.bookbridge.dto.ApiResponse;
import com.bookbridge.dto.DisputeRequest;
import com.bookbridge.dto.DisputeResponse;
import com.bookbridge.security.UserPrincipal;
import com.bookbridge.service.DisputeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputes")
@Tag(name = "Dispute Management", description = "Endpoints for students to raise claim complaints and administrators to review and resolve them")
public class DisputeController {

    private final DisputeService disputeService;

    @Autowired
    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    @Operation(summary = "Raise a purchase dispute complaint", description = "Opens a new dispute case for a completed textbook transaction. Only authorized to the buyer.")
    public ResponseEntity<ApiResponse<DisputeResponse>> raiseDispute(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @Valid @RequestBody DisputeRequest request) {
        DisputeResponse response = disputeService.raiseDispute(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Dispute case logged successfully.", response));
    }

    @GetMapping("/{disputeId}")
    @Operation(summary = "View dispute case details", description = "Returns detailed reasons and reviews for active participants.")
    public ResponseEntity<ApiResponse<DisputeResponse>> getDetails(@AuthenticationPrincipal UserPrincipal principal,
                                                                   @PathVariable Long disputeId) {
        DisputeResponse response = disputeService.getDisputeDetails(disputeId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Dispute case details retrieved.", response));
    }

    @GetMapping
    @Operation(summary = "View dispute claims list", description = "Students receive their personal complaints list. Administrators receive the complete system review queue.")
    public ResponseEntity<ApiResponse<List<DisputeResponse>>> getDisputes(@AuthenticationPrincipal UserPrincipal principal) {
        List<DisputeResponse> response = disputeService.getDisputeHistory(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Dispute claims list retrieved successfully.", response));
    }

    @PutMapping("/{disputeId}/resolve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Update dispute status & resolve claim (Admin Only)", description = "Administrators audit dispute tickets. Resolving a dispute triggers a mock refund log to the buyer.")
    public ResponseEntity<ApiResponse<DisputeResponse>> resolveDispute(@AuthenticationPrincipal UserPrincipal principal,
                                                                       @PathVariable Long disputeId,
                                                                       @RequestParam String status) {
        DisputeResponse response = disputeService.updateDisputeStatus(disputeId, status, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Dispute case status resolved successfully.", response));
    }
}
