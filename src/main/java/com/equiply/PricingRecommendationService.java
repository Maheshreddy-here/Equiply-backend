package com.equiply;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Transparent, rule-based first version of Equiply's pricing assistant. */
@Service
public class PricingRecommendationService {

    public Recommendation recommend(BigDecimal itemValue, String category, String condition) {
        if (itemValue == null || itemValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Estimated item value must be greater than zero.");
        }

        double categoryFactor = categoryFactor(category);
        double conditionFactor = conditionFactor(condition);
        // Start at 1.5% of replacement value per day, then adjust for category and condition.
        BigDecimal rent = itemValue.multiply(BigDecimal.valueOf(0.015 * categoryFactor * conditionFactor));
        BigDecimal minRent = BigDecimal.valueOf(50);
        BigDecimal maxRent = itemValue.multiply(BigDecimal.valueOf(0.05));
        if (rent.compareTo(minRent) < 0) rent = minRent;
        if (rent.compareTo(maxRent) > 0 && maxRent.compareTo(BigDecimal.ZERO) > 0) rent = maxRent;
        rent = roundToTen(rent);

        double depositFactor = "NEW".equalsIgnoreCase(condition) ? 0.25 :
                "GOOD".equalsIgnoreCase(condition) ? 0.20 : 0.15;
        BigDecimal deposit = itemValue.multiply(BigDecimal.valueOf(depositFactor));
        if (deposit.compareTo(BigDecimal.valueOf(100)) < 0) deposit = BigDecimal.valueOf(100);
        deposit = deposit.setScale(0, RoundingMode.HALF_UP);

        String explanation = String.format(Locale.ENGLISH,
                "Estimated from item value, category (%s) and condition (%s). Treat this as a starting point and compare similar local listings.",
                safe(category), safe(condition));
        return new Recommendation(rent, deposit, explanation);
    }

    private double categoryFactor(String category) {
        if (category == null) return 1.0;
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "electronics", "laptop", "camera", "photography" -> 1.15;
            case "tools", "engineering tools" -> 1.05;
            case "sports", "sports gear" -> 0.90;
            case "books", "academic", "academic items" -> 0.65;
            default -> 1.0;
        };
    }

    private double conditionFactor(String condition) {
        if (condition == null) return 1.0;
        return switch (condition.toUpperCase(Locale.ROOT)) {
            case "NEW" -> 1.15;
            case "GOOD" -> 1.0;
            case "FAIR" -> 0.75;
            default -> 1.0;
        };
    }

    private BigDecimal roundToTen(BigDecimal amount) {
        return amount.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).multiply(BigDecimal.TEN);
    }

    private String safe(String value) { return value == null || value.isBlank() ? "general" : value; }

    public record Recommendation(BigDecimal rentPerDay, BigDecimal deposit, String explanation) {}
}
