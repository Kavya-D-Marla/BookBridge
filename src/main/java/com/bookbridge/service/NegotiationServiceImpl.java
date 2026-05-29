package com.bookbridge.service;

import com.bookbridge.algorithms.NegotiationRecommendation;
import com.bookbridge.dto.NegotiationResponse;
import com.bookbridge.dto.OfferRequest;
import com.bookbridge.dto.OfferResponse;
import com.bookbridge.entity.*;
import com.bookbridge.exception.BadRequestException;
import com.bookbridge.exception.ResourceNotFoundException;
import com.bookbridge.exception.UnauthorizedException;
import com.bookbridge.repository.BookRepository;
import com.bookbridge.repository.NegotiationRepository;
import com.bookbridge.repository.OfferRepository;
import com.bookbridge.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NegotiationServiceImpl implements NegotiationService {

    private final NegotiationRepository negotiationRepository;
    private final OfferRepository offerRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final NegotiationRecommendation negotiationRecommendationAlgorithm;

    @Autowired
    public NegotiationServiceImpl(NegotiationRepository negotiationRepository,
                                  OfferRepository offerRepository,
                                  BookRepository bookRepository,
                                  UserRepository userRepository,
                                  NegotiationRecommendation negotiationRecommendationAlgorithm) {
        this.negotiationRepository = negotiationRepository;
        this.offerRepository = offerRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.negotiationRecommendationAlgorithm = negotiationRecommendationAlgorithm;
    }

    @Override
    @Transactional
    public NegotiationResponse initiateOrSendOffer(Long bookId, Long buyerId, OfferRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book", "id", bookId));

        if (book.getStatus() == BookStatus.SOLD) {
            throw new BadRequestException("This textbook is already sold! Bidding is closed.");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (book.getSeller().getUserId().equals(buyerId)) {
            throw new BadRequestException("You cannot buy or negotiate on your own listed textbook!");
        }

        // Find existing negotiation or create new one
        Negotiation negotiation = negotiationRepository.findByBookAndBuyer(book, buyer)
                .orElseGet(() -> {
                    Negotiation newNegotiation = Negotiation.builder()
                            .book(book)
                            .buyer(buyer)
                            .seller(book.getSeller())
                            .status(NegotiationStatus.OPEN)
                            .offers(new ArrayList<>())
                            .build();
                    return negotiationRepository.save(newNegotiation);
                });

        if (negotiation.getStatus() != NegotiationStatus.OPEN) {
            // Re-open negotiation if previously rejected or expired
            negotiation.setStatus(NegotiationStatus.OPEN);
        }

        // Enforce alternating turns if offers already exist
        if (!negotiation.getOffers().isEmpty()) {
            Offer lastOffer = negotiation.getOffers().get(negotiation.getOffers().size() - 1);
            if (lastOffer.getUser().getUserId().equals(buyerId)) {
                throw new BadRequestException("You already made the last offer! Wait for the seller to respond.");
            }
        }

        Offer offer = Offer.builder()
                .negotiation(negotiation)
                .user(buyer)
                .offeredPrice(request.getOfferedPrice())
                .message(request.getMessage())
                .build();

        Offer savedOffer = offerRepository.save(offer);
        negotiation.getOffers().add(savedOffer);
        negotiationRepository.save(negotiation);

        log.info("Student {} placed offer of {} INR on textbook '{}'", 
                buyer.getEmail(), request.getOfferedPrice(), book.getTitle());

        return mapToNegotiationResponse(negotiation);
    }

    @Override
    @Transactional
    public NegotiationResponse sendCounterOffer(Long negotiationId, Long userId, OfferRequest request) {
        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (negotiation.getStatus() != NegotiationStatus.OPEN) {
            throw new BadRequestException("This negotiation is no longer open for active bidding.");
        }

        if (negotiation.getBook().getStatus() == BookStatus.SOLD) {
            throw new BadRequestException("The book is already sold.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isBuyer = negotiation.getBuyer().getUserId().equals(userId);
        boolean isSeller = negotiation.getSeller().getUserId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new UnauthorizedException("You are not authorized to participate in this negotiation.");
        }

        // Enforce turn-taking
        Offer lastOffer = negotiation.getOffers().get(negotiation.getOffers().size() - 1);
        if (lastOffer.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("You cannot submit consecutive offers. Wait for the other party to respond.");
        }

        Offer counterOffer = Offer.builder()
                .negotiation(negotiation)
                .user(user)
                .offeredPrice(request.getOfferedPrice())
                .message(request.getMessage())
                .build();

        Offer savedOffer = offerRepository.save(counterOffer);
        negotiation.getOffers().add(savedOffer);
        negotiationRepository.save(negotiation);

        log.info("User {} placed counter-offer of {} INR on negotiation ID: {}", 
                user.getEmail(), request.getOfferedPrice(), negotiationId);

        return mapToNegotiationResponse(negotiation);
    }

    @Override
    @Transactional
    public NegotiationResponse acceptOffer(Long negotiationId, Long userId) {
        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (negotiation.getStatus() != NegotiationStatus.OPEN) {
            throw new BadRequestException("This negotiation is not open.");
        }

        Book book = negotiation.getBook();
        if (book.getStatus() == BookStatus.SOLD) {
            throw new BadRequestException("Textbook already sold.");
        }

        boolean isBuyer = negotiation.getBuyer().getUserId().equals(userId);
        boolean isSeller = negotiation.getSeller().getUserId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new UnauthorizedException("You are not authorized to accept this deal.");
        }

        // Make sure the last offer was submitted by the *other* party
        Offer lastOffer = negotiation.getOffers().get(negotiation.getOffers().size() - 1);
        if (lastOffer.getUser().getUserId().equals(userId)) {
            throw new BadRequestException("You cannot accept your own offer! Wait for the other party to accept.");
        }

        // Transition status
        negotiation.setStatus(NegotiationStatus.ACCEPTED);
        book.setStatus(BookStatus.RESERVED); // Reserve the book so checkouts can happen
        
        bookRepository.save(book);
        Negotiation savedNegotiation = negotiationRepository.save(negotiation);

        log.info("Negotiation ID: {} accepted! Price agreed: {} INR. Listing reserved.", 
                negotiationId, lastOffer.getOfferedPrice());

        return mapToNegotiationResponse(savedNegotiation);
    }

    @Override
    @Transactional
    public NegotiationResponse rejectOffer(Long negotiationId, Long userId) {
        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (negotiation.getStatus() != NegotiationStatus.OPEN) {
            throw new BadRequestException("Negotiation already completed.");
        }

        boolean isBuyer = negotiation.getBuyer().getUserId().equals(userId);
        boolean isSeller = negotiation.getSeller().getUserId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new UnauthorizedException("Unauthorized.");
        }

        negotiation.setStatus(NegotiationStatus.REJECTED);
        Negotiation savedNegotiation = negotiationRepository.save(negotiation);
        
        log.info("Negotiation ID: {} was rejected by User ID: {}", negotiationId, userId);
        return mapToNegotiationResponse(savedNegotiation);
    }

    @Override
    public BigDecimal getCounterOfferRecommendation(Long negotiationId, Long userId) {
        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        boolean isSeller = negotiation.getSeller().getUserId().equals(userId);
        boolean isBuyer = negotiation.getBuyer().getUserId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new UnauthorizedException("Unauthorized.");
        }

        BigDecimal askingPrice = negotiation.getBook().getAskingPrice();
        BigDecimal lastOfferPrice = BigDecimal.ZERO;

        if (!negotiation.getOffers().isEmpty()) {
            lastOfferPrice = negotiation.getOffers().get(negotiation.getOffers().size() - 1).getOfferedPrice();
        }

        // Call algorithm recommendation
        return negotiationRecommendationAlgorithm.suggestCounterOffer(askingPrice, lastOfferPrice, isSeller);
    }

    @Override
    public NegotiationResponse getNegotiationDetails(Long negotiationId, Long userId) {
        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (!negotiation.getBuyer().getUserId().equals(userId) && 
            !negotiation.getSeller().getUserId().equals(userId) &&
            !userRepository.findById(userId).get().getRole().name().equals("ROLE_ADMIN")) {
            throw new UnauthorizedException("You are not part of this negotiation thread.");
        }

        return mapToNegotiationResponse(negotiation);
    }

    @Override
    public List<NegotiationResponse> getMyNegotiations(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Negotiation> negotiations = new ArrayList<>();
        negotiations.addAll(negotiationRepository.findByBuyer(user));
        negotiations.addAll(negotiationRepository.findBySeller(user));

        // Deduplicate in case of overlays, sorted by creation date newest first
        return negotiations.stream()
                .distinct()
                .sorted((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()))
                .map(this::mapToNegotiationResponse)
                .collect(Collectors.toList());
    }

    private NegotiationResponse mapToNegotiationResponse(Negotiation negotiation) {
        List<OfferResponse> offerResponses = negotiation.getOffers().stream()
                .map(offer -> OfferResponse.builder()
                        .offerId(offer.getOfferId())
                        .negotiationId(negotiation.getNegotiationId())
                        .userId(offer.getUser().getUserId())
                        .userName(offer.getUser().getFirstName() + " " + offer.getUser().getLastName())
                        .offeredPrice(offer.getOfferedPrice())
                        .message(offer.getMessage())
                        .timestamp(offer.getTimestamp())
                        .build())
                .collect(Collectors.toList());

        return NegotiationResponse.builder()
                .negotiationId(negotiation.getNegotiationId())
                .bookId(negotiation.getBook().getBookId())
                .bookTitle(negotiation.getBook().getTitle())
                .askingPrice(negotiation.getBook().getAskingPrice())
                .buyerId(negotiation.getBuyer().getUserId())
                .buyerName(negotiation.getBuyer().getFirstName() + " " + negotiation.getBuyer().getLastName())
                .sellerId(negotiation.getSeller().getUserId())
                .sellerName(negotiation.getSeller().getFirstName() + " " + negotiation.getSeller().getLastName())
                .status(negotiation.getStatus().name())
                .offers(offerResponses)
                .createdAt(negotiation.getCreatedAt())
                .build();
    }
}
