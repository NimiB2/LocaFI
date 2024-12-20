package dev.nimrod.locafi.models;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class LocationState {
    private static final String TAG = "LocationState";
    private static LocationState instance;
    private LatLng lastLocation;
    private List<WifiPoint> lastWifiPoints;
    private Map<String, Integer> lastSignalStrengths;
    private static final int DEFAULT_RSSI_THRESHOLD = 5;
    private static final long SIGNAL_TIMEOUT = 30000; // 30 seconds
    private long lastUpdateTime;

    private LocationState() {
        lastSignalStrengths = new HashMap<>();
        lastWifiPoints = new ArrayList<>();
    }

    public static LocationState getInstance() {
        if (instance == null) {
            instance = new LocationState();
        }
        return instance;
    }

    public LatLng getLastLocation() {
        if (System.currentTimeMillis() - lastUpdateTime > SIGNAL_TIMEOUT) {
            // Reset state if too much time has passed
            reset();
            return null;
        }
        return lastLocation;
    }

    public void setLastLocation(LatLng location) {
        this.lastLocation = location;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void reset() {
        lastLocation = null;
        lastWifiPoints = new ArrayList<>();
        lastSignalStrengths.clear();
        Log.d(TAG, "Location state reset");
    }

    public boolean hasSignificantChange(List<WifiPoint> newWifiPoints) {
        if (newWifiPoints == null) {
            return false;
        }

        if (lastWifiPoints.isEmpty()) {
            updateWifiState(newWifiPoints);
            return true;
        }

        // Check for network changes
        if (hasNetworkSetChanged(newWifiPoints)) {
            Log.d(TAG, "Network set has changed");
            updateWifiState(newWifiPoints);
            return true;
        }

        // Check for significant RSSI changes
        if (hasSignificantRssiChange(newWifiPoints)) {
            Log.d(TAG, "Significant RSSI change detected");
            updateWifiState(newWifiPoints);
            return true;
        }

        return false;
    }

    private boolean hasNetworkSetChanged(List<WifiPoint> newPoints) {
        Set<String> currentBssids = lastWifiPoints.stream()
                .map(WifiPoint::getBssid)
                .collect(Collectors.toSet());

        Set<String> newBssids = newPoints.stream()
                .map(WifiPoint::getBssid)
                .collect(Collectors.toSet());

        return !currentBssids.equals(newBssids);
    }

    private boolean hasSignificantRssiChange(List<WifiPoint> newPoints) {
        for (WifiPoint newPoint : newPoints) {
            Integer lastRssi = lastSignalStrengths.get(newPoint.getBssid());
            if (lastRssi != null) {
                int rssiDifference = Math.abs(newPoint.getRssi() - lastRssi);
                if (rssiDifference > DEFAULT_RSSI_THRESHOLD) {
                    Log.d(TAG, String.format("RSSI change detected for %s: %d -> %d",
                            newPoint.getBssid(), lastRssi, newPoint.getRssi()));
                    return true;
                }
            }
        }
        return false;
    }

    private void updateWifiState(List<WifiPoint> newPoints) {
        lastWifiPoints = new ArrayList<>(newPoints);
        lastSignalStrengths.clear();
        for (WifiPoint point : newPoints) {
            lastSignalStrengths.put(point.getBssid(), point.getRssi());
        }
        lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isLocationStale() {
        return System.currentTimeMillis() - lastUpdateTime > SIGNAL_TIMEOUT;
    }
}