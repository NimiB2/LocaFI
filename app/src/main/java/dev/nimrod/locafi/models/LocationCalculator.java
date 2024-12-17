package dev.nimrod.locafi.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LocationCalculator {
    private static final double EARTH_RADIUS = 6371000; // Earth's radius in meters

    /**
     * Calculate possible user locations based on WiFi points
     * @param wifiPoints List of detected WiFi points
     * @return List of possible locations with their probabilities
     */
    public static List<WeightedLocation> calculatePossibleLocations(List<WifiPoint> wifiPoints) {
        if (wifiPoints == null || wifiPoints.isEmpty()) {
            return new ArrayList<>();
        }

        if (wifiPoints.size() == 1) {
            return calculateSinglePointLocation(wifiPoints.get(0));
        }

        return calculateMultiPointLocation(wifiPoints);
    }

    /**
     * Calculate possible locations for a single WiFi point
     * @param wifiPoint Single WiFi point
     * @return List of points on the circle with equal probability
     */
    private static List<WeightedLocation> calculateSinglePointLocation(WifiPoint wifiPoint) {
        List<WeightedLocation> locations = new ArrayList<>();
        double distance = wifiPoint.getDistance();

        // Generate points on the circle at regular intervals
        int numPoints = 36; // Every 10 degrees
        for (int i = 0; i < numPoints; i++) {
            double angle = (2 * Math.PI * i) / numPoints;
            double lat = wifiPoint.getLatitude() + (distance * Math.cos(angle)) / EARTH_RADIUS;
            double lng = wifiPoint.getLongitude() +
                    (distance * Math.sin(angle)) / (EARTH_RADIUS * Math.cos(Math.toRadians(wifiPoint.getLatitude())));

            locations.add(new WeightedLocation(new LatLng(lat, lng), 1.0 / numPoints));
        }

        return locations;
    }

    /**
     * Calculate intersection points between multiple WiFi circles
     * @param wifiPoints List of WiFi points
     * @return List of intersection points with their probabilities
     */
    private static List<WeightedLocation> calculateMultiPointLocation(List<WifiPoint> wifiPoints) {
        List<WeightedLocation> intersectionPoints = new ArrayList<>();

        // Calculate intersections between each pair of circles
        for (int i = 0; i < wifiPoints.size() - 1; i++) {
            for (int j = i + 1; j < wifiPoints.size(); j++) {
                List<LatLng> points = findCircleIntersections(
                        wifiPoints.get(i),
                        wifiPoints.get(j)
                );

                // Weight points based on signal strength and distance
                for (LatLng point : points) {
                    double weight = calculateLocationWeight(point, wifiPoints);
                    intersectionPoints.add(new WeightedLocation(point, weight));
                }
            }
        }

        // If no intersections found, calculate intermediate points
        if (intersectionPoints.isEmpty()) {
            return calculateIntermediatePoints(wifiPoints);
        }

        return normalizeWeights(intersectionPoints);
    }

    /**
     * Calculate weight for a location based on all WiFi points
     */
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

    /**
     * Calculate intermediate points when circles don't intersect
     */
    private static List<WeightedLocation> calculateIntermediatePoints(List<WifiPoint> wifiPoints) {
        List<WeightedLocation> intermediatePoints = new ArrayList<>();

        // Find weighted center point
        double totalWeight = 0;
        double weightedLat = 0;
        double weightedLng = 0;

        for (WifiPoint point : wifiPoints) {
            double weight = Math.exp(point.getRssi() / 20.0); // Stronger signal = higher weight
            weightedLat += point.getLatitude() * weight;
            weightedLng += point.getLongitude() * weight;
            totalWeight += weight;
        }

        LatLng centerPoint = new LatLng(
                weightedLat / totalWeight,
                weightedLng / totalWeight
        );

        intermediatePoints.add(new WeightedLocation(centerPoint, 1.0));
        return intermediatePoints;
    }

    /**
     * Find intersection points between two WiFi circles
     */
    private static List<LatLng> findCircleIntersections(WifiPoint p1, WifiPoint p2) {
        double d = calculateDistance(
                new LatLng(p1.getLatitude(), p1.getLongitude()),
                new LatLng(p2.getLatitude(), p2.getLongitude())
        );

        double r1 = p1.getDistance();
        double r2 = p2.getDistance();

        List<LatLng> intersections = new ArrayList<>();

        // Check if circles are separate or one contains the other
        if (d > r1 + r2 || d < Math.abs(r1 - r2)) {
            return intersections;
        }

        // Calculate intersection points
        double a = (r1 * r1 - r2 * r2 + d * d) / (2 * d);
        double h = Math.sqrt(r1 * r1 - a * a);

        // Calculate intermediate point
        double x2 = p1.getLongitude() + (a * (p2.getLongitude() - p1.getLongitude())) / d;
        double y2 = p1.getLatitude() + (a * (p2.getLatitude() - p1.getLatitude())) / d;

        // Calculate intersection points
        double x3 = x2 + (h * (p2.getLatitude() - p1.getLatitude())) / d;
        double y3 = y2 - (h * (p2.getLongitude() - p1.getLongitude())) / d;

        double x4 = x2 - (h * (p2.getLatitude() - p1.getLatitude())) / d;
        double y4 = y2 + (h * (p2.getLongitude() - p1.getLongitude())) / d;

        intersections.add(new LatLng(y3, x3));
        intersections.add(new LatLng(y4, x4));

        return intersections;
    }

    /**
     * Calculate distance between two points in meters
     */
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

    /**
     * Normalize weights to sum to 1.0
     */
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

    /**
     * Class to hold a location with its probability weight
     */
    public static class WeightedLocation {
        public final LatLng location;
        public final double weight;

        public WeightedLocation(LatLng location, double weight) {
            this.location = location;
            this.weight = weight;
        }
    }
}