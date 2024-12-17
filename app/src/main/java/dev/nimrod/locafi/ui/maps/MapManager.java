package dev.nimrod.locafi.ui.maps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

import dev.nimrod.locafi.R;
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
    private Marker wifiMarker;

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

        map.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null; // Default window
            }

            @Override
            public View getInfoContents(Marker marker) {
                LinearLayout infoView = new LinearLayout(context);
                infoView.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(context);
                title.setText(marker.getTitle());
                title.setTextColor(Color.BLACK);
                title.setTextSize(16);
                infoView.addView(title);

                return infoView;
            }
        });
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

    private BitmapDescriptor getBitmapDescriptorFromVector(int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public void updateUserLocation(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userMarker == null) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(userLatLng)
                    .title("Real GPS Location")
                    .icon(getBitmapDescriptorFromVector(R.drawable.gps_location_icon));

            userMarker = map.addMarker(markerOptions);
        } else {
            userMarker.setPosition(userLatLng);
        }
    }

    public void updateWifiBasedLocation(LatLng wifiLocation) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(wifiLocation)
                .title("WiFi-Based Location")
                .icon(getBitmapDescriptorFromVector(R.drawable.wifi_location_icon));

        map.addMarker(markerOptions);
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
