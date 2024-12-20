package dev.nimrod.locafi.ui.maps;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.LocationCalculator;
import dev.nimrod.locafi.models.LocationComparisonState;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.models.WifiTriangulation;

public class MapManager {
    private GoogleMap map;
    private List<Circle> wifiCircles;
    private List<Marker> wifiMarkers;

    private Marker gpsMarker;
    private Polyline comparisonLine;

    private Marker distanceMarker;
    private static final float MIN_ACCURACY_THRESHOLD = 20.0f; // meters

    private LocationComparisonState comparisonState;
    private static final int GPS_MARKER_SIZE = 48;

    //private Marker userMarker;
    private Polygon intersectionArea;
    private Map<String, WifiPoint> wifiPointMap;
    private Context context;
    private Marker wifiMarker;
    private static final int WIFI_DOT_SIZE = 12; // Small dot for WiFi
    private static final int LOCATION_MARKER_SIZE = 48;

    private static final int ANIMATION_DURATION = 1000; // 1 second
    private Circle accuracyCircle;
    private Handler animationHandler = new Handler(Looper.getMainLooper());

    public MapManager(GoogleMap map, Context context) {
        this.map = map;
        this.context = context;
        this.wifiCircles = new ArrayList<>();
        this.wifiMarkers = new ArrayList<>();
        this.wifiPointMap = new HashMap<>();
        this.comparisonState = new LocationComparisonState();
        setupMap();
    }

    private void setupMap() {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(false);  // Disable location button

        // Remove all location UI but keep functionality
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(false);
        }

    }


    private BitmapDescriptor createDotMarker(int size, int color) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void zoomToLocation(LatLng location) {
        if (map != null && location != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(location)
                    .zoom(18f)
                    .build();
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    private BitmapDescriptor createLocationMarker(int size, int color) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Outer circle
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        // Inner circle
        paint.setColor(Color.WHITE);
        canvas.drawCircle(radius, radius, radius * 0.6f, paint);

        // Center
        paint.setColor(color);
        canvas.drawCircle(radius, radius, radius * 0.3f, paint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    public void updateWifiPoints(List<WifiPoint> wifiPoints) {
        clearMap();
        wifiPointMap.clear();

        // Draw WiFi points and their circles
        for (WifiPoint point : wifiPoints) {
            if (!point.hasValidPosition()) continue;

            // Create marker for WiFi point
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(point.getPosition().toLatLng())
                    .title(point.getSsid())
                    .snippet(String.format("Signal: %d dBm", point.getRssi()))
                    .icon(createDotMarker(WIFI_DOT_SIZE, Color.BLUE))
                    .anchor(0.5f, 0.5f);

            Marker marker = map.addMarker(markerOptions);
            wifiMarkers.add(marker);

            // Create signal range circle
            CircleOptions circleOptions = new CircleOptions()
                    .center(point.getPosition().toLatLng())
                    .radius(point.getDistance())
                    .strokeWidth(3)
                    .strokeColor(Color.parseColor(point.getSignalColor()))
                    .fillColor(Color.TRANSPARENT);

            Circle circle = map.addCircle(circleOptions);
            wifiCircles.add(circle);
            wifiPointMap.put(marker.getId(), point);
        }

        // Calculate and show estimated position
        List<LocationCalculator.WeightedLocation> locations =
                LocationCalculator.calculatePossibleLocations(wifiPoints);

        if (!locations.isEmpty()) {
            LocationCalculator.WeightedLocation bestLocation = locations.get(0);
            updateEstimatedLocation(bestLocation.location, wifiPoints);
        }
    }

    private void updateEstimatedLocation(LatLng location, List<WifiPoint> wifiPoints) {
        if (wifiMarker == null) {
            // Create marker without animation or camera movement
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location)
                    .title("WIFI LOCATION")
                    .icon(createLocationMarker(LOCATION_MARKER_SIZE, Color.parseColor("#FF8C00")))
                    .anchor(0.5f, 0.5f)
                    .zIndex(2.0f);
            wifiMarker = map.addMarker(markerOptions);
            // Only zoom first time
            zoomToLocation(location);
        } else {
            // Just update marker position without any animation or camera movement
            wifiMarker.setPosition(location);
        }
    }

    // Simplify the animation method
    private void animateMarkerMovement(LatLng start, LatLng end) {
        if (start == null || end == null) return;

        final long startTime = SystemClock.uptimeMillis();
        final double latDiff = end.latitude - start.latitude;
        final double lngDiff = end.longitude - start.longitude;

        animationHandler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = Math.min((float) elapsed / ANIMATION_DURATION, 1f);

                float interpolatedFraction = interpolateEaseInOut(t);

                double lat = start.latitude + (latDiff * interpolatedFraction);
                double lng = start.longitude + (lngDiff * interpolatedFraction);
                LatLng newPosition = new LatLng(lat, lng);
                wifiMarker.setPosition(newPosition);

                if (t < 1f) {
                    animationHandler.post(this);
                }
            }
        });
    }
    private void animateMarkerMovement(LatLng start, LatLng end, double finalRadius) {
        if (start == null || end == null) return;

        final long startTime = SystemClock.uptimeMillis();
        final float startRadius = (float) accuracyCircle.getRadius();
        final double latDiff = end.latitude - start.latitude;
        final double lngDiff = end.longitude - start.longitude;
        final double radiusDiff = finalRadius - startRadius;

        animationHandler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = Math.min((float) elapsed / ANIMATION_DURATION, 1f);

                // Interpolate using acceleration/deceleration
                float interpolatedFraction = interpolateEaseInOut(t);

                // Update marker position
                double lat = start.latitude + (latDiff * interpolatedFraction);
                double lng = start.longitude + (lngDiff * interpolatedFraction);
                LatLng newPosition = new LatLng(lat, lng);
                wifiMarker.setPosition(newPosition);

                // Update accuracy circle
                accuracyCircle.setCenter(newPosition);
                accuracyCircle.setRadius(startRadius + (radiusDiff * interpolatedFraction));

                // Continue animation if not finished
                if (t < 1f) {
                    animationHandler.post(this);
                }
            }
        });
    }

    private float interpolateEaseInOut(float t) {
        return t < 0.5f ?
                2 * t * t :
                -1 + (4 - 2 * t) * t;
    }


    private BitmapDescriptor getBitmapDescriptorFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        // Increase size
        vectorDrawable.setBounds(0, 0,
                vectorDrawable.getIntrinsicWidth() * 2, // doubled size
                vectorDrawable.getIntrinsicHeight() * 2);
        Bitmap bitmap = Bitmap.createBitmap(
                vectorDrawable.getIntrinsicWidth() * 2,
                vectorDrawable.getIntrinsicHeight() * 2,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void updateUserLocation(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

//        if (userMarker == null) {
//            MarkerOptions markerOptions = new MarkerOptions()
//                    .position(userLatLng)
//                    .title("Real GPS Location")
//                    .icon(getBitmapDescriptorFromVector(R.drawable.gps_location_icon));
//
//            userMarker = map.addMarker(markerOptions);
//        } else {
//            userMarker.setPosition(userLatLng);
//        }
    }

    public void updateWifiBasedLocation(LatLng wifiLocation) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(wifiLocation)
                .title("WiFi-Based Location")
                .icon(getBitmapDescriptorFromVector(R.drawable.wifi_location_icon));


        if (wifiMarker != null) {
            wifiMarker.remove();
        }
        wifiMarker = map.addMarker(markerOptions);
    }

    private double calculateAccuracyRadius(List<WifiPoint> wifiPoints) {
        if (wifiPoints == null || wifiPoints.isEmpty()) {
            return 50.0; // Default radius in meters if no points
        }

        // Get average signal strength
        double avgSignalStrength = wifiPoints.stream()
                .mapToInt(WifiPoint::getRssi)
                .average()
                .orElse(-85);  // Default to weak signal if empty

        // Convert to radius: stronger signals = smaller radius
        // -50 dBm (very strong) = ~15m radius
        // -85 dBm (weak) = ~50m radius
        double radius = 15 + Math.abs(avgSignalStrength + 50) * 1.0;

        // Bound the radius between 15 and 50 meters
        return Math.min(Math.max(radius, 15), 50);
    }

    private void zoomToFitAll(List<WifiPoint> wifiPoints) {
        if (wifiPoints.isEmpty()) return;

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLng = Double.MIN_VALUE;

        for (WifiPoint point : wifiPoints) {
            double lat = point.getLatitude();
            double lng = point.getLongitude();

            minLat = Math.min(minLat, lat);
            maxLat = Math.max(maxLat, lat);
            minLng = Math.min(minLng, lng);
            maxLng = Math.max(maxLng, lng);
        }

        double maxDistance = wifiPoints.stream()
                .mapToDouble(WifiPoint::getDistance)
                .max()
                .orElse(100);

        double padding = maxDistance / 111000;

        map.animateCamera(CameraUpdateFactory.newLatLngBounds(
                new com.google.android.gms.maps.model.LatLngBounds(
                        new LatLng(minLat - padding, minLng - padding),
                        new LatLng(maxLat + padding, maxLng + padding)
                ), 50));
    }

    public void clearMap() {
        for (Circle circle : wifiCircles) {
            circle.remove();
        }
        wifiCircles.clear();

        for (Marker marker : wifiMarkers) {
            marker.remove();
        }
        wifiMarkers.clear();

        if (wifiMarker != null) {
            wifiMarker.remove();
            wifiMarker = null;
        }
        if (accuracyCircle != null) {
            accuracyCircle.remove();
            accuracyCircle = null;
        }
        clearComparisonVisualization();
    }

    public void toggleLocationComparison(boolean enabled) {
        comparisonState.setEnabled(enabled);
        if (!enabled) {
            clearComparisonVisualization();
        }
    }

    public void updateComparisonVisualization(Location gpsLocation) {
        if (!comparisonState.isEnabled() || gpsLocation == null) return;

        if (gpsLocation.getAccuracy() > MIN_ACCURACY_THRESHOLD) {
            showAccuracyWarning();
            return;
        }

        LatLng gpsLatLng = new LatLng(gpsLocation.getLatitude(), gpsLocation.getLongitude());
        LatLng wifiLatLng = wifiMarker != null ? wifiMarker.getPosition() : null;

        if (wifiLatLng != null) {
            comparisonState.updateLocations(gpsLatLng, wifiLatLng);
            updateGpsMarker(gpsLatLng);
            updateComparisonLine(gpsLatLng, wifiLatLng);
            updateDistanceInfo();
        }
    }

    private void showAccuracyWarning() {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                Snackbar.make(
                        ((Activity) context).findViewById(android.R.id.content),
                        "Waiting for better GPS accuracy...",
                        Snackbar.LENGTH_SHORT
                ).show();
            });
        }
    }
    private void updateGpsMarker(LatLng position) {
        if (gpsMarker == null) {
            MarkerOptions options = new MarkerOptions()
                    .position(position)
                    .icon(createLocationMarker(GPS_MARKER_SIZE, Color.RED))
                    .title("GPS Location")
                    .anchor(0.5f, 0.5f);
            gpsMarker = map.addMarker(options);
        } else {
            gpsMarker.setPosition(position);
        }
    }

    private void updateComparisonLine(LatLng gpsPos, LatLng wifiPos) {
        List<LatLng> points = Arrays.asList(gpsPos, wifiPos);

        if (comparisonLine == null) {
            PolylineOptions options = new PolylineOptions()
                    .addAll(points)
                    .color(Color.BLUE)
                    .width(5)
                    .pattern(Arrays.asList(
                            new Dot(), new Gap(20)
                    ));
            comparisonLine = map.addPolyline(options);
        } else {
            comparisonLine.setPoints(points);
        }
    }

    private void updateDistanceInfo() {
        String distanceText = String.format("Distance: %.2f m", comparisonState.getDistance());
        LatLng midPoint = getMidPoint(
                comparisonState.getGpsLocation(),
                comparisonState.getWifiLocation()
        );

        if (distanceMarker == null) {
            MarkerOptions options = new MarkerOptions()
                    .position(midPoint)
                    .icon(BitmapDescriptorFactory.fromBitmap(createTextBitmap(distanceText)))
                    .anchor(0.5f, 0.5f)
                    .zIndex(1.0f); // Make sure it stays on top
            distanceMarker = map.addMarker(options);
        } else {
            distanceMarker.setPosition(midPoint);
            distanceMarker.setIcon(BitmapDescriptorFactory.fromBitmap(
                    createTextBitmap(distanceText)));
        }
    }
    private void clearComparisonVisualization() {
        if (gpsMarker != null) {
            gpsMarker.remove();
            gpsMarker = null;
        }
        if (comparisonLine != null) {
            comparisonLine.remove();
            comparisonLine = null;
        }
        if (distanceMarker != null) {
            distanceMarker.remove();
            distanceMarker = null;
        }
    }

    private LatLng getMidPoint(LatLng p1, LatLng p2) {
        return new LatLng(
                (p1.latitude + p2.latitude) / 2,
                (p1.longitude + p2.longitude) / 2
        );
    }

    private Bitmap createTextBitmap(String text) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(35);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);

        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);

        Bitmap image = Bitmap.createBitmap(width + 20, height + 10, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.WHITE);
        canvas.drawText(text, width/2 + 10, baseline + 5, paint);

        return image;
    }



    public WifiPoint getWifiPoint(Marker marker) {
        return wifiPointMap.get(marker.getId());
    }
}
