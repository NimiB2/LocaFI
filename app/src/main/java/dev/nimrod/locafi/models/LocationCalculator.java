package dev.nimrod.locafi.models;

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

        if (wifiPoints.size() == 1) {
            return calculateSinglePointLocation(wifiPoints.get(0));
        }

        return calculateMultiPointLocation(wifiPoints);
    }

    private static List<WeightedLocation> calculateSinglePointLocation(WifiPoint wifiPoint) {
        // For single WiFi, return a single random point on the circle edge
        double angle = Math.random() * 2 * Math.PI;
        double distance = wifiPoint.getDistance();

        double lat = wifiPoint.getLatitude() +
                (distance * Math.cos(angle)) / EARTH_RADIUS;
        double lng = wifiPoint.getLongitude() +
                (distance * Math.sin(angle)) / (EARTH_RADIUS * Math.cos(Math.toRadians(wifiPoint.getLatitude())));

        List<WeightedLocation> locations = new ArrayList<>();
        locations.add(new WeightedLocation(new LatLng(lat, lng), 1.0));
        return locations;
    }


    private static List<WeightedLocation> calculateMultiPointLocation(List<WifiPoint> wifiPoints) {
        List<WeightedLocation> possibleLocations = new ArrayList<>();

        // Try to find intersection points first
        boolean hasIntersections = false;
        for (int i = 0; i < wifiPoints.size() - 1; i++) {
            for (int j = i + 1; j < wifiPoints.size(); j++) {
                List<LatLng> intersections = findCircleIntersections(wifiPoints.get(i), wifiPoints.get(j));
                if (!intersections.isEmpty()) {
                    hasIntersections = true;
                    for (LatLng point : intersections) {
                        double weight = calculateLocationWeight(point, wifiPoints);
                        possibleLocations.add(new WeightedLocation(point, weight));
                    }
                }
            }
        }

        // If no intersections, calculate weighted point and project to nearest circle edge
        if (!hasIntersections) {
            WeightedLocation weightedCenter = calculateWeightedMiddleLocation(wifiPoints);
            // Find the strongest WiFi point
            WifiPoint strongestPoint = findStrongestWifiPoint(wifiPoints);
            // Project the weighted center to the edge of the strongest WiFi's circle
            LatLng projectedPoint = projectToCircleEdge(
                    weightedCenter.location,
                    new LatLng(strongestPoint.getLatitude(), strongestPoint.getLongitude()),
                    strongestPoint.getDistance()
            );
            possibleLocations.add(new WeightedLocation(projectedPoint, 1.0));
        }

        return normalizeWeights(possibleLocations);
    }

    private static WifiPoint findStrongestWifiPoint(List<WifiPoint> wifiPoints) {
        return Collections.max(wifiPoints, (a, b) -> Integer.compare(a.getRssi(), b.getRssi()));
    }


    private static WeightedLocation calculateWeightedMiddleLocation(List<WifiPoint> wifiPoints) {
        double totalWeight = 0;
        double weightedLat = 0;
        double weightedLng = 0;

        for (WifiPoint point : wifiPoints) {
            // Use signal strength as weight
            double weight = Math.exp(point.getRssi() / 20.0); // Stronger signal = higher weight
            weightedLat += point.getLatitude() * weight;
            weightedLng += point.getLongitude() * weight;
            totalWeight += weight;
        }

        return new WeightedLocation(
                new LatLng(
                        weightedLat / totalWeight,
                        weightedLng / totalWeight
                ),
                1.0
        );
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


    private static LatLng projectToCircleEdge(LatLng point, LatLng center, double radius) {
        double bearing = calculateBearing(center, point);

        double lat1 = Math.toRadians(center.latitude);
        double lng1 = Math.toRadians(center.longitude);
        double angularDistance = radius / EARTH_RADIUS;

        double lat2 = Math.asin(
                Math.sin(lat1) * Math.cos(angularDistance) +
                        Math.cos(lat1) * Math.sin(angularDistance) * Math.cos(bearing)
        );

        double lng2 = lng1 + Math.atan2(
                Math.sin(bearing) * Math.sin(angularDistance) * Math.cos(lat1),
                Math.cos(angularDistance) - Math.sin(lat1) * Math.sin(lat2)
        );

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lng2));
    }

    private static double calculateBearing(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lat2 = Math.toRadians(to.latitude);
        double dLng = Math.toRadians(to.longitude - from.longitude);

        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);

        return Math.atan2(y, x);
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