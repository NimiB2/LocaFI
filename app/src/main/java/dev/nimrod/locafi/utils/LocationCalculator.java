package dev.nimrod.locafi.utils;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;
import dev.nimrod.locafi.models.WiFiDevice;

public class LocationCalculator {
    private static final int BASE_RADIUS = 100; // Base radius in meters
    private static final int MIN_SIGNAL = -100; // Minimum signal strength in dBm

    public static LatLng calculateLocation(List<WiFiDevice> devices) {
        if (devices == null || devices.isEmpty()) {
            return null;
        }

        // For single device, return its location
        if (devices.size() == 1) {
            WiFiDevice device = devices.get(0);
            return new LatLng(device.getLatitude(), device.getLongitude());
        }

        // For multiple devices, calculate weighted average
        double totalWeight = 0;
        double weightedLat = 0;
        double weightedLon = 0;

        for (WiFiDevice device : devices) {
            double weight = calculateWeight(device.getSignalStrength());
            weightedLat += device.getLatitude() * weight;
            weightedLon += device.getLongitude() * weight;
            totalWeight += weight;
        }

        return new LatLng(
                weightedLat / totalWeight,
                weightedLon / totalWeight
        );
    }

    private static double calculateWeight(int signalStrength) {
        // Convert signal strength to a weight (stronger signal = higher weight)
        // Normalize signal strength to 0-1 range
        double normalizedSignal = Math.abs((double) (signalStrength - MIN_SIGNAL) / MIN_SIGNAL);
        return Math.pow(normalizedSignal, 2); // Square it to emphasize stronger signals
    }
}