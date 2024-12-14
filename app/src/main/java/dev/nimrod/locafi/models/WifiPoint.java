package dev.nimrod.locafi.models;

import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;

public class WifiPoint {

    private String ssid;
    private String bssid;
    private int rssi;
    private double distance;

    public WifiPoint(String ssid, String bssid, int rssi, double distance) {
        this.ssid = ssid;
        this.bssid = bssid;
        this.rssi = rssi;
        this.distance = distance;
    }

    // Getters and setters
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
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    // Utility methods
    public int getSignalLevel(int numLevels) {
        return WifiManager.calculateSignalLevel(rssi, numLevels);
    }

    @NonNull
    @Override
    public String toString() {
        return "WifiPoint{" +
                "ssid='" + ssid + '\'' +
                ", bssid='" + bssid + '\'' +
                ", rssi=" + rssi +
                ", distance=" + String.format("%.2f", distance) +
                '}';
    }
}
