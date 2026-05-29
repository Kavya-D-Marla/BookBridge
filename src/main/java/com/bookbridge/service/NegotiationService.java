package com.bookbridge.service;

import com.bookbridge.dto.NegotiationResponse;
import com.bookbridge.dto.OfferRequest;

import java.math.BigDecimal;
import java.util.List;

public interface NegotiationService {
    NegotiationResponse initiateOrSendOffer(Long bookId, Long buyerId, OfferRequest request);
    NegotiationResponse sendCounterOffer(Long negotiationId, Long userId, OfferRequest request);
    NegotiationResponse acceptOffer(Long negotiationId, Long userId);
    NegotiationResponse rejectOffer(Long negotiationId, Long userId);
    BigDecimal getCounterOfferRecommendation(Long negotiationId, Long userId);
    NegotiationResponse getNegotiationDetails(Long negotiationId, Long userId);
    List<NegotiationResponse> getMyNegotiations(Long userId);
}
