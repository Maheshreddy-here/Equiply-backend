package com.equiply;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PricingRecommendationServiceTest {
    private final PricingRecommendationService service = new PricingRecommendationService();

    @Test
    void recommendsRentAndDepositForAnItem() {
        var recommendation = service.recommend(new BigDecimal("10000"), "Electronics", "GOOD");
        assertTrue(recommendation.rentPerDay().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(recommendation.deposit().compareTo(BigDecimal.ZERO) > 0);
        assertNotNull(recommendation.explanation());
    }

    @Test
    void rejectsZeroItemValue() {
        assertThrows(IllegalArgumentException.class,
                () -> service.recommend(BigDecimal.ZERO, "Books", "GOOD"));
    }
}
