package dev.nimrod.locafi.models;

import com.google.android.gms.maps.model.LatLng;

public class LocationComparisonState {
    private boolean isEnabled;
    private LatLng gpsLocation;
    private LatLng wifiLocation;
    private double distance;

    public LocationComparisonState() {
        this.isEnabled = false;
    }

    public void updateLocations(LatLng gps, LatLng wifi) {
        this.gpsLocation = gps;
        this.wifiLocation = wifi;
        calculateDistance();
    }

    private void calculateDistance() {
        if (gpsLocation != null && wifiLocation != null) {
            float[] results = new float[1];
            android.location.Location.distanceBetween(
                    gpsLocation.latitude, gpsLocation.longitude,
                    wifiLocation.latitude, wifiLocation.longitude,
                    results);
            this.distance = results[0];
        }
    }

    // Getters and setters
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public LatLng getGpsLocation() {
        return gpsLocation;
    }

    public LatLng getWifiLocation() {
        return wifiLocation;
    }

    public double getDistance() {
        return distance;
    }
}