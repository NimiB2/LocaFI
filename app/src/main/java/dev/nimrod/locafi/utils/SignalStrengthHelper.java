package dev.nimrod.locafi.utils;

import android.graphics.Color;

public class SignalStrengthHelper {
    // Constants for signal strength ranges
    private static final int EXCELLENT_SIGNAL = -50;
    private static final int GOOD_SIGNAL = -60;
    private static final int FAIR_SIGNAL = -70;
    private static final int POOR_SIGNAL = -80;

    // Constants for circle radius (in meters)
    private static final float EXCELLENT_RADIUS = 10;
    private static final float GOOD_RADIUS = 20;
    private static final float FAIR_RADIUS = 30;
    private static final float POOR_RADIUS = 40;
    private static final float VERY_POOR_RADIUS = 50;

    public static int calculateSignalLevel(int rssi) {
        if (rssi >= EXCELLENT_SIGNAL) return 4; // Excellent
        if (rssi >= GOOD_SIGNAL) return 3;      // Good
        if (rssi >= FAIR_SIGNAL) return 2;      // Fair
        if (rssi >= POOR_SIGNAL) return 1;      // Poor
        return 0;                               // Very poor
    }

    public static int getColorForSignalStrength(int rssi) {
        switch (calculateSignalLevel(rssi)) {
            case 4: return Color.rgb(0, 255, 0);     // Green for excellent
            case 3: return Color.rgb(144, 238, 144); // Light green for good
            case 2: return Color.rgb(255, 215, 0);   // Yellow for fair
            case 1: return Color.rgb(255, 165, 0);   // Orange for poor
            default: return Color.rgb(255, 0, 0);    // Red for very poor
        }
    }

    public static float getCircleRadius(int rssi) {
        switch (calculateSignalLevel(rssi)) {
            case 4: return EXCELLENT_RADIUS;
            case 3: return GOOD_RADIUS;
            case 2: return FAIR_RADIUS;
            case 1: return POOR_RADIUS;
            default: return VERY_POOR_RADIUS;
        }
    }
}