package com.equiply;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RentalAccessServiceTest {
    private final RentalAccessService service = new RentalAccessService();

    private User user(Long id, String college, double lat, double lon) {
        User user = new User();
        user.setId(id);
        user.setCollegeName(college);
        user.setLatitude(lat);
        user.setLongitude(lon);
        return user;
    }

    @Test void sameCollegeCanAccessListing() {
        assertTrue(service.canAccess(user(1L, "Anurag University", 17.0, 78.0),
                user(2L, " anurag-university ", 18.0, 79.0)));
    }

    @Test void differentCollegeWithinTenKmCanAccessListing() {
        assertTrue(service.canAccess(user(1L, "College A", 17.0000, 78.0000),
                user(2L, "College B", 17.0450, 78.0000)));
    }

    @Test void differentCollegeOutsideTenKmCannotAccessListing() {
        assertFalse(service.canAccess(user(1L, "College A", 17.0000, 78.0000),
                user(2L, "College B", 17.2000, 78.0000)));
    }
}
