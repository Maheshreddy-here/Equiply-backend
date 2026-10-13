package com.equiply;

import org.springframework.stereotype.Service;

/** Applies Equiply's local-community rule to both listing visibility and booking requests. */
@Service
public class RentalAccessService {
    public static final double PICKUP_RADIUS_KM = 10.0;
    private static final double EARTH_RADIUS_KM = 6371.0088;

    /**
     * A renter can see/request a listing if they are in the same college community,
     * or their saved campus/pickup area is within 10 km of the owner's saved area.
     */
    public boolean canAccess(User renter, User owner) {
        if (renter == null || owner == null) return false;
        if (renter.getId() != null && renter.getId().equals(owner.getId())) return true;
        if (sameCollege(renter.getCollegeName(), owner.getCollegeName())) return true;
        Double distance = distanceKm(renter, owner);
        return distance != null && distance <= PICKUP_RADIUS_KM;
    }

    public boolean sameCollege(String first, String second) {
        return first != null && second != null && normalize(first).equals(normalize(second));
    }

    /** Returns null when either user's coordinates are not available. */
    public Double distanceKm(User first, User second) {
        if (first == null || second == null || first.getLatitude() == null || first.getLongitude() == null
                || second.getLatitude() == null || second.getLongitude() == null) return null;
        double lat1 = Math.toRadians(first.getLatitude());
        double lat2 = Math.toRadians(second.getLatitude());
        double deltaLat = Math.toRadians(second.getLatitude() - first.getLatitude());
        double deltaLon = Math.toRadians(second.getLongitude() - first.getLongitude());
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double boundedA = Math.min(1.0, Math.max(0.0, a));
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(boundedA), Math.sqrt(1 - boundedA));
    }

    private String normalize(String value) {
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
