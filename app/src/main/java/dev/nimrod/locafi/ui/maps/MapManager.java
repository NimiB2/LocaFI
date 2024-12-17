package dev.nimrod.locafi.ui.maps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.nimrod.locafi.models.LocationCalculator;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.models.WifiTriangulation;

public class MapManager {
    private GoogleMap map;
    private List<Circle> wifiCircles;
    private List<Marker> wifiMarkers;
    private Marker userMarker;
    private Polygon intersectionArea;
    private Map<String, WifiPoint> wifiPointMap;
    private Context context;

    public MapManager(GoogleMap map, Context context) {
        this.map = map;
        this.context = context;
        this.wifiCircles = new ArrayList<>();
        this.wifiMarkers = new ArrayList<>();
        this.wifiPointMap = new HashMap<>();
        setupMap();
    }

    private void setupMap() {
        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMyLocationButtonEnabled(true);

        // Add these lines:
        map.setMinZoomPreference(15); // Set minimum zoom level
        map.setMaxZoomPreference(20); // Set maximum zoom level

        // Enable my location layer if permission is granted
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
        }
    }

    public void updateWifiPoints(List<WifiPoint> wifiPoints) {
        clearMap();
        wifiPointMap.clear();

        for (WifiPoint point : wifiPoints) {
            if (!point.hasValidPosition()) continue;

            MarkerOptions markerOptions = new MarkerOptions()
                    .position(point.getPosition().toLatLng())
                    .title(point.getSsid())
                    .snippet("Signal: " + point.getRssi() + " dBm\n" +
                            "Distance: " + String.format("%.2f m", point.getDistance()));

            Marker marker = map.addMarker(markerOptions);
            wifiMarkers.add(marker);

            CircleOptions circleOptions = new CircleOptions()
                    .center(point.getPosition().toLatLng())
                    .radius(point.getDistance())
                    .strokeWidth(2)
                    .strokeColor(Color.parseColor(point.getSignalColor()))
                    .fillColor(Color.parseColor(point.getSignalColor()));

            Circle circle = map.addCircle(circleOptions);
            wifiCircles.add(circle);

            wifiPointMap.put(marker.getId(), point);
        }

        if (wifiPoints.size() > 1) {
            updateIntersectionArea(wifiPoints);
        }

        if (!wifiPoints.isEmpty()) {
            zoomToFitAll(wifiPoints);
        }
    }

    public void updateUserLocation(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(userLatLng)
                    .title("Your Location")
                    .snippet("Accuracy: " + String.format("%.2f m", location.getAccuracy()));

            userMarker = map.addMarker(markerOptions);
        } else {
            userMarker.setPosition(userLatLng);
            userMarker.setSnippet("Accuracy: " + String.format("%.2f m", location.getAccuracy()));
        }
    }

    private void updateIntersectionArea(List<WifiPoint> wifiPoints) {
        if (intersectionArea != null) {
            intersectionArea.remove();
        }

        WifiTriangulation triangulation = new WifiTriangulation(wifiPoints);
        List<LatLng> intersectionPoints = calculateIntersectionPolygon(triangulation);

        if (!intersectionPoints.isEmpty()) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(intersectionPoints)
                    .strokeWidth(2)
                    .strokeColor(Color.BLUE)
                    .fillColor(Color.argb(50, 0, 0, 255));

            intersectionArea = map.addPolygon(polygonOptions);
        }
    }

    private List<LatLng> calculateIntersectionPolygon(WifiTriangulation triangulation) {
        List<LocationCalculator.WeightedLocation> weightedLocations =
                triangulation.getPossibleLocations();

        List<LatLng> polygonPoints = new ArrayList<>();

        for (LocationCalculator.WeightedLocation loc : weightedLocations) {
            if (loc.weight > 0.1) {
                polygonPoints.add(loc.location);
            }
        }

        return polygonPoints;
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

        if (intersectionArea != null) {
            intersectionArea.remove();
            intersectionArea = null;
        }
    }

    public WifiPoint getWifiPoint(Marker marker) {
        return wifiPointMap.get(marker.getId());
    }
}
