package dev.nimrod.locafi.models;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

public class WifiPosition {
    private double latitude;
    private double longitude;
    private double accuracy; // Accuracy radius in meters
    private long timestamp; // When position was last updated

    public WifiPosition(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    public WifiPosition(double latitude, double longitude, double accuracy) {
        this(latitude, longitude);
        this.accuracy = accuracy;
    }

    public WifiPosition(LatLng latLng) {
        this(latLng.latitude, latLng.longitude);
    }

    /**
     * Calculate distance to another position in meters
     */
    public double distanceTo(WifiPosition other) {
        final int R = 6371000; // Earth's radius in meters

        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double lng1 = Math.toRadians(this.longitude);
        double lng2 = Math.toRadians(other.longitude);

        double dlat = lat2 - lat1;
        double dlng = lng2 - lng1;

        double a = Math.sin(dlat/2) * Math.sin(dlat/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dlng/2) * Math.sin(dlng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R * c;
    }

    /**
     * Check if this position is within a certain radius of another position
     */
    public boolean isWithinRadius(WifiPosition other, double radiusMeters) {
        return distanceTo(other) <= radiusMeters;
    }

    /**
     * Convert to Google Maps LatLng
     */
    public LatLng toLatLng() {
        return new LatLng(latitude, longitude);
    }

    /**
     * Create a new position at a given distance and bearing from this position
     * @param distanceMeters Distance in meters
     * @param bearingDegrees Bearing in degrees (0 = north, 90 = east, etc.)
     */
    public WifiPosition calculateDestination(double distanceMeters, double bearingDegrees) {
        final int R = 6371000; // Earth's radius in meters

        double bearing = Math.toRadians(bearingDegrees);
        double lat1 = Math.toRadians(latitude);
        double lng1 = Math.toRadians(longitude);
        double angDist = distanceMeters / R;

        double lat2 = Math.asin(
                Math.sin(lat1) * Math.cos(angDist) +
                        Math.cos(lat1) * Math.sin(angDist) * Math.cos(bearing)
        );

        double lng2 = lng1 + Math.atan2(
                Math.sin(bearing) * Math.sin(angDist) * Math.cos(lat1),
                Math.cos(angDist) - Math.sin(lat1) * Math.sin(lat2)
        );

        // Normalize longitude to -180 to +180 degrees
        lng2 = (lng2 + Math.PI) % (2 * Math.PI) - Math.PI;

        return new WifiPosition(Math.toDegrees(lat2), Math.toDegrees(lng2));
    }

    /**
     * Check if position is valid (within reasonable bounds)
     */
    public boolean isValid() {
        return latitude >= -90 && latitude <= 90 &&
                longitude >= -180 && longitude <= 180;
    }

    /**
     * Calculate age of position in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - timestamp;
    }

    // Getters and setters
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
        this.timestamp = System.currentTimeMillis();
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("WifiPosition{lat=%f, lng=%f, accuracy=%f meters}",
                latitude, longitude, accuracy);
    }
}
