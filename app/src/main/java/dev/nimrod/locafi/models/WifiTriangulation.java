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


    private void calculatePossibleLocations() {
        this.possibleLocations = LocationCalculator.calculatePossibleLocations(wifiPoints);
    }


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


    public List<LocationCalculator.WeightedLocation> getPossibleLocations() {
        return possibleLocations;
    }

    public void setUserLocation(LatLng location) {
        this.userLocation = location;
    }

    public LatLng getUserLocation() {
        return userLocation;
    }

    public List<WifiPoint> getWifiPoints() {
        return wifiPoints;
    }
}