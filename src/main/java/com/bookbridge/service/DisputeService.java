package com.bookbridge.service;

import com.bookbridge.dto.DisputeRequest;
import com.bookbridge.dto.DisputeResponse;

import java.util.List;

public interface DisputeService {
    DisputeResponse raiseDispute(DisputeRequest request, Long buyerId);
    DisputeResponse updateDisputeStatus(Long disputeId, String status, Long adminId);
    List<DisputeResponse> getDisputeHistory(Long userId);
    DisputeResponse getDisputeDetails(Long disputeId, Long userId);
}
