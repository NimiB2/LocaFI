package dev.nimrod.locafi.models;

import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

public class WifiPoint {
    private String ssid;
    private String bssid;
    private int rssi;
    private double distance;
    private WifiPosition position;
    private int signalLevel; // Signal level from 0 to 4
    private static final int MAX_SIGNAL_LEVEL = 4;

    public WifiPoint(String ssid, String bssid, int rssi, double distance) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.distance = distance;
        this.signalLevel = calculateSignalLevel();
    }

    public WifiPoint(String ssid, String bssid, int rssi, double distance, WifiPosition position) {
        this(ssid, bssid, rssi, distance);
        this.position = position;
    }

    /**
     * Calculate signal level based on RSSI value
     * @return signal level from 0 to 4
     */
    private int calculateSignalLevel() {
        return WifiManager.calculateSignalLevel(rssi, MAX_SIGNAL_LEVEL + 1);
    }

    /**
     * Get signal color based on signal level
     * @return hex color string
     */
    public String getSignalColor() {
        switch (signalLevel) {
            case 4:
                return "#4CAF50"; // Strong - Green
            case 3:
                return "#8BC34A"; // Good - Light Green
            case 2:
                return "#FFC107"; // Fair - Yellow
            case 1:
                return "#FF9800"; // Poor - Orange
            default:
                return "#F44336"; // Very Poor - Red
        }
    }

    /**
     * Calculate circle opacity based on signal strength
     * @return opacity value between 0.2 and 0.6
     */
    public double getCircleOpacity() {
        return 0.2 + (signalLevel * 0.1); // 0.2 to 0.6 based on signal level
    }

    /**
     * Check if the WiFi point has a valid position
     */
    public boolean hasValidPosition() {
        return position != null && position.isValid();
    }

    /**
     * Get the confidence radius based on signal strength and distance
     * Stronger signals have smaller radius (more confident)
     * @return radius in meters
     */
    public double getConfidenceRadius() {
        // Base radius on distance and signal strength
        double baseRadius = distance * (1 + (MAX_SIGNAL_LEVEL - signalLevel) * 0.2);
        // Add minimum radius to account for measurement uncertainty
        return Math.max(baseRadius, 5.0); // Minimum 5 meters radius
    }

    public boolean isValidPosition() {
        return position != null && position.isValid();
    }


    // Enhanced getters and setters
    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getBssid() {
        return bssid;
    }

    public void setBssid(String bssid) {
        this.bssid = bssid;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
        this.signalLevel = calculateSignalLevel();
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public WifiPosition getPosition() {
        return position;
    }

    public void setPosition(WifiPosition position) {
        this.position = position;
    }

    public int getSignalLevel() {
        return signalLevel;
    }

    public double getLatitude() {
        return position != null ? position.getLatitude() : 0;
    }

    public double getLongitude() {
        return position != null ? position.getLongitude() : 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "WifiPoint{" +
                "ssid='" + ssid + '\'' +
                ", bssid='" + bssid + '\'' +
                ", rssi=" + rssi +
                ", distance=" + String.format("%.2f", distance) +
                ", signalLevel=" + signalLevel +
                ", position=" + (position != null ? position.toString() : "null") +
                '}';
    }
}