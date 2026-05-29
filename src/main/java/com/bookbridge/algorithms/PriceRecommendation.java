package com.bookbridge.algorithms;

import com.bookbridge.entity.BookCondition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
public class PriceRecommendation {

    /**
     * Suggests a fair secondhand textbook selling price based on conditions and age.
     * 
     * @param originalMSRP Original price of the book when bought new.
     * @param condition    Current condition of the textbook (NEW, LIKE_NEW, GOOD, ACCEPTABLE).
     * @param publishedYear The year the textbook was published.
     * @return Suggested fair resale price, rounded to 2 decimal places.
     */
    public BigDecimal suggestPrice(BigDecimal originalMSRP, BookCondition condition, int publishedYear) {
        if (originalMSRP == null || originalMSRP.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 1. Condition Multiplier
        double conditionFactor = switch (condition) {
            case NEW -> 0.85;       // 15% discount for already open/bought new
            case LIKE_NEW -> 0.70;  // 30% discount
            case GOOD -> 0.55;      // 45% discount
            case ACCEPTABLE -> 0.35; // 65% discount
        };

        // 2. Age-Based Depreciation (5% depreciation per year, capped at 50% total depreciation)
        int currentYear = LocalDate.now().getYear();
        int age = Math.max(0, currentYear - publishedYear);
        double ageDepreciation = Math.min(0.50, age * 0.05);
        double ageFactor = 1.0 - ageDepreciation;

        // 3. Calculate Final Price
        double rawSuggestedPrice = originalMSRP.doubleValue() * conditionFactor * ageFactor;
        
        // Ensure price is at least 10% of original MSRP to avoid near-zero values for very old books
        double minPrice = originalMSRP.doubleValue() * 0.10;
        double finalPrice = Math.max(rawSuggestedPrice, minPrice);

        return BigDecimal.valueOf(finalPrice).setScale(2, RoundingMode.HALF_UP);
    }
}
