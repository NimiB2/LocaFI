package dev.nimrod.locafi.models;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocationCalculator {
    private static final double EARTH_RADIUS = 6371000; // Earth's radius in meters

    public static List<WeightedLocation> calculatePossibleLocations(List<WifiPoint> wifiPoints) {
        if (wifiPoints == null || wifiPoints.isEmpty()) {
            return new ArrayList<>();
        }

        List<WeightedLocation> result = new ArrayList<>();

        if (wifiPoints.size() == 1) {
            // For single WiFi, place point on circle edge
            WifiPoint point = wifiPoints.get(0);

            if (!point.hasValidPosition()) {
                return result;
            }

            // Random angle between 0 and 360 degrees
            double angle = Math.random() * 2 * Math.PI;
            double distance = point.getDistance();

            // Calculate position on circle edge
            double lat = point.getLatitude() +
                    (distance * Math.cos(angle)) / 111111.0; // Convert meters to degrees
            double lng = point.getLongitude() +
                    (distance * Math.sin(angle)) / (111111.0 * Math.cos(Math.toRadians(point.getLatitude())));

            result.add(new WeightedLocation(new LatLng(lat, lng), 1.0));
            return result;
        } else {
            // Multiple WiFi points - use weighted centroid
            double totalWeight = 0;
            double weightedLat = 0;
            double weightedLng = 0;

            for (WifiPoint point : wifiPoints) {
                if (!point.hasValidPosition()) {
                    continue;
                }

                // Convert RSSI to weight (stronger signal = higher weight)
                // Add 100 to make all values positive, then exp to emphasize stronger signals
                double weight = Math.exp((point.getRssi() + 100) / 10.0);

                weightedLat += point.getLatitude() * weight;
                weightedLng += point.getLongitude() * weight;
                totalWeight += weight;
            }

            // If no valid points were found, return empty list
            if (totalWeight == 0) {
                return result;
            }

            // Calculate the weighted center
            LatLng estimatedLocation = new LatLng(
                    weightedLat / totalWeight,
                    weightedLng / totalWeight
            );

            result.add(new WeightedLocation(estimatedLocation, 1.0));
            return result;
        }
    }
    private static WifiPoint findStrongestWifiPoint(List<WifiPoint> wifiPoints) {
        return Collections.max(wifiPoints, (a, b) -> Integer.compare(a.getRssi(), b.getRssi()));
    }


    private static double calculateLocationWeight(LatLng location, List<WifiPoint> wifiPoints) {
        double weight = 1.0;
        for (WifiPoint point : wifiPoints) {
            double distance = calculateDistance(
                    location,
                    new LatLng(point.getLatitude(), point.getLongitude())
            );
            double expectedDistance = point.getDistance();
            double difference = Math.abs(distance - expectedDistance);

            // Weight decreases exponentially with difference from expected distance
            weight *= Math.exp(-difference / expectedDistance);
        }
        return weight;
    }

    private static double calculateDistance(LatLng point1, LatLng point2) {
        double lat1 = Math.toRadians(point1.latitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lng1 = Math.toRadians(point1.longitude);
        double lng2 = Math.toRadians(point2.longitude);

        double dlat = lat2 - lat1;
        double dlng = lng2 - lng1;

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dlng/2) * Math.sin(dlng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return EARTH_RADIUS * c;
    }


    private static List<WeightedLocation> normalizeWeights(List<WeightedLocation> locations) {
        double totalWeight = 0;
        for (WeightedLocation location : locations) {
            totalWeight += location.weight;
        }

        List<WeightedLocation> normalized = new ArrayList<>();
        for (WeightedLocation location : locations) {
            normalized.add(new WeightedLocation(
                    location.location,
                    location.weight / totalWeight
            ));
        }

        return normalized;
    }



    public static class WeightedLocation {
        public final LatLng location;
        public final double weight;

        public WeightedLocation(LatLng location, double weight) {
            this.location = location;
            this.weight = weight;
        }
    }
}