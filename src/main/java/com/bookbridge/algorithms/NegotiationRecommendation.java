package com.bookbridge.algorithms;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class NegotiationRecommendation {

    /**
     * Recommends a fair counteroffer price during dynamic book price bargaining.
     *
     * @param askingPrice    The original asking price set by the seller.
     * @param lastOfferPrice The last offered price submitted (either by buyer or seller counter).
     * @param isSeller       True if the user requesting the counter-offer recommendation is the seller, false if buyer.
     * @return Suggested counteroffer price, rounded to 2 decimal places.
     */
    public BigDecimal suggestCounterOffer(BigDecimal askingPrice, BigDecimal lastOfferPrice, boolean isSeller) {
        if (askingPrice == null || askingPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (lastOfferPrice == null || lastOfferPrice.compareTo(BigDecimal.ZERO) <= 0) {
            // No valid previous offer. Recommend a standard starting discount or negotiation boundary.
            if (isSeller) {
                return askingPrice; // Sellers default to asking price
            } else {
                // Buyers start at 80% of asking price as a recommended initial offer
                return askingPrice.multiply(BigDecimal.valueOf(0.80)).setScale(2, RoundingMode.HALF_UP);
            }
        }

        // If last offer is already higher than asking price (rare user behavior)
        if (lastOfferPrice.compareTo(askingPrice) >= 0) {
            return askingPrice;
        }

        // Difference between asking price and current bid
        BigDecimal gap = askingPrice.subtract(lastOfferPrice);

        BigDecimal counterOffer;
        if (isSeller) {
            // Sellers want to concede some ground but retain higher value.
            // Suggest counter-offering by lowering the price to: lastOfferPrice + 60% of the gap
            BigDecimal concessions = gap.multiply(BigDecimal.valueOf(0.60));
            counterOffer = lastOfferPrice.add(concessions);
        } else {
            // Buyers want to raise their bid but stay low.
            // Suggest counter-offering by raising the bid to: lastOfferPrice + 40% of the gap
            BigDecimal increase = gap.multiply(BigDecimal.valueOf(0.40));
            counterOffer = lastOfferPrice.add(increase);
        }

        return counterOffer.setScale(2, RoundingMode.HALF_UP);
    }
}
