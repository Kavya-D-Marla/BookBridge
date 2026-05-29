package com.bookbridge.controller;

import com.bookbridge.dto.ApiResponse;
import com.bookbridge.dto.NegotiationResponse;
import com.bookbridge.dto.OfferRequest;
import com.bookbridge.security.UserPrincipal;
import com.bookbridge.service.NegotiationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/negotiations")
@Tag(name = "Structured Negotiation System", description = "Endpoints for dynamic bidding, alternating counteroffers, acceptances, and bargaining guides")
public class NegotiationController {

    private final NegotiationService negotiationService;

    @Autowired
    public NegotiationController(NegotiationService negotiationService) {
        this.negotiationService = negotiationService;
    }

    @PostMapping("/initiate/{bookId}")
    @Operation(summary = "Submit a buying bid / place initial offer", description = "Buyer places an initial price bid on listed textbook. Creates a stateful negotiation thread.")
    public ResponseEntity<ApiResponse<NegotiationResponse>> initiateOrOffer(@AuthenticationPrincipal UserPrincipal principal,
                                                                             @PathVariable Long bookId,
                                                                             @Valid @RequestBody OfferRequest request) {
        NegotiationResponse response = negotiationService.initiateOrSendOffer(bookId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Offer submitted successfully.", response));
    }

    @PostMapping("/{negotiationId}/counter")
    @Operation(summary = "Place a counter-offer", description = "Submits a counter-offer in the bargaining flow. System enforces turn-taking so consecutive offers are blocked.")
    public ResponseEntity<ApiResponse<NegotiationResponse>> counterOffer(@AuthenticationPrincipal UserPrincipal principal,
                                                                         @PathVariable Long negotiationId,
                                                                         @Valid @RequestBody OfferRequest request) {
        NegotiationResponse response = negotiationService.sendCounterOffer(negotiationId, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Counter-offer submitted successfully.", response));
    }

    @PostMapping("/{negotiationId}/accept")
    @Operation(summary = "Accept current price offer", description = "Accepts the last counter-offer of the other party. Transitions negotiation status to ACCEPTED and reserves listing stock.")
    public ResponseEntity<ApiResponse<NegotiationResponse>> acceptOffer(@AuthenticationPrincipal UserPrincipal principal,
                                                                        @PathVariable Long negotiationId) {
        NegotiationResponse response = negotiationService.acceptOffer(negotiationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Offer accepted successfully. Listing is reserved for checkout.", response));
    }

    @PostMapping("/{negotiationId}/reject")
    @Operation(summary = "Reject pricing negotiation", description = "Closes negotiation thread with REJECTED status.")
    public ResponseEntity<ApiResponse<NegotiationResponse>> rejectOffer(@AuthenticationPrincipal UserPrincipal principal,
                                                                        @PathVariable Long negotiationId) {
        NegotiationResponse response = negotiationService.rejectOffer(negotiationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Negotiation rejected.", response));
    }

    @GetMapping("/{negotiationId}/recommend")
    @Operation(summary = "Calculate recommended counter-offer", description = "Uses the Negotiation Recommendation Algorithm, suggesting a midpoint counter based on role limits and gap sizes.")
    public ResponseEntity<ApiResponse<BigDecimal>> recommendCounter(@AuthenticationPrincipal UserPrincipal principal,
                                                                    @PathVariable Long negotiationId) {
        BigDecimal recommendation = negotiationService.getCounterOfferRecommendation(negotiationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Suggested counter-offer value calculated.", recommendation));
    }

    @GetMapping("/{negotiationId}")
    @Operation(summary = "Fetch negotiation complete thread", description = "Returns bidding logs and details for active participants.")
    public ResponseEntity<ApiResponse<NegotiationResponse>> getNegotiationDetails(@AuthenticationPrincipal UserPrincipal principal,
                                                                                  @PathVariable Long negotiationId) {
        NegotiationResponse response = negotiationService.getNegotiationDetails(negotiationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Negotiation details retrieved.", response));
    }

    @GetMapping
    @Operation(summary = "List active negotiations", description = "Returns active bargaining threads where the student is either the buyer or seller.")
    public ResponseEntity<ApiResponse<List<NegotiationResponse>>> getMyNegotiations(@AuthenticationPrincipal UserPrincipal principal) {
        List<NegotiationResponse> response = negotiationService.getMyNegotiations(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("My negotiations threads retrieved.", response));
    }
}
