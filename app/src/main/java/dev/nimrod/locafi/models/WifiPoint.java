package dev.nimrod.locafi.models;

import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

public class WifiPoint {
    private String ssid;
    private String bssid;
    private int rssi;
    private double distance;
    private WifiPosition position;
    private int signalLevel;
    private static final int MAX_SIGNAL_LEVEL = 4;

    public WifiPoint(String ssid, String bssid, int rssi) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.signalLevel = calculateSignalLevel();
        calculateDistance();
    }


    private int calculateSignalLevel() {
        return WifiManager.calculateSignalLevel(rssi, MAX_SIGNAL_LEVEL + 1);
    }



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


    public double getCircleOpacity() {
        return 0.2 + (signalLevel * 0.1); // 0.2 to 0.6 based on signal level
    }


    public boolean hasValidPosition() {
        return position != null && position.isValid();
    }

    public void calculateDistance() {
        // Environmental factor (typically between 2.0 to 4.0)
        double environmentalFactor = 2.4;

        // Reference RSSI at 1 meter distance (calibration value)
        int referenceRSSI = -40;

        // Calculate distance using log-distance path loss model
        this.distance = Math.pow(10.0, (referenceRSSI - rssi) / (10 * environmentalFactor));

        // Add bounds to prevent unrealistic values
        this.distance = Math.min(Math.max(this.distance, 1.0), 50.0);
    }

    public double getConfidenceRadius() {
        // Base radius on distance and signal strength
        double baseRadius = distance * (1 + (MAX_SIGNAL_LEVEL - signalLevel) * 0.2);
        // Add minimum radius to account for measurement uncertainty
        return Math.max(baseRadius, 5.0); // Minimum 5 meters radius
    }

    public boolean isValidPosition() {
        return position != null && position.isValid();
    }

    public int getSignalStrengthPercentage() {
        return (signalLevel * 100) / MAX_SIGNAL_LEVEL;
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