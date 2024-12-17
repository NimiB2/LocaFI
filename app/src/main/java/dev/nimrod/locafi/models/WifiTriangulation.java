package dev.nimrod.locafi.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class WifiTriangulation {
    private List<WifiPoint> wifiPoints;
    private List<LocationCalculator.WeightedLocation> possibleLocations;
    private LatLng userLocation;

    public WifiTriangulation(List<WifiPoint> wifiPoints) {
        this.wifiPoints = wifiPoints;
        calculatePossibleLocations();
    }

    /**
     * Calculate possible user locations based on WiFi points
     */
    private void calculatePossibleLocations() {
        this.possibleLocations = LocationCalculator.calculatePossibleLocations(wifiPoints);
    }

    /**
     * Get the most likely user location
     */
    public LatLng getMostLikelyLocation() {
        if (possibleLocations == null || possibleLocations.isEmpty()) {
            return null;
        }

        LocationCalculator.WeightedLocation mostLikely = possibleLocations.get(0);
        for (LocationCalculator.WeightedLocation location : possibleLocations) {
            if (location.weight > mostLikely.weight) {
                mostLikely = location;
            }
        }

        return mostLikely.location;
    }

    /**
     * Get all possible locations with their probabilities
     */
    public List<LocationCalculator.WeightedLocation> getPossibleLocations() {
        return possibleLocations;
    }

    /**
     * Set actual user location for comparison
     */
    public void setUserLocation(LatLng location) {
        this.userLocation = location;
    }

    /**
     * Get actual user location
     */
    public LatLng getUserLocation() {
        return userLocation;
    }

    /**
     * Get WiFi points used in triangulation
     */
    public List<WifiPoint> getWifiPoints() {
        return wifiPoints;
    }
}