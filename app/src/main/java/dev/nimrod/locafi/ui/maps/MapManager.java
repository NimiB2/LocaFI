package dev.nimrod.locafi.ui.maps;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
    //private Marker userMarker;
    private Polygon intersectionArea;
    private Map<String, WifiPoint> wifiPointMap;
    private Context context;
    private Marker wifiMarker;
    private static final int WIFI_DOT_SIZE = 12; // Small dot for WiFi
    private static final int LOCATION_MARKER_SIZE = 48;

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

    private BitmapDescriptor createLocationMarker(int size, int color) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);

        // Draw outer circle
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        // Draw inner circle
        paint.setColor(Color.WHITE);
        canvas.drawCircle(radius, radius, radius/2, paint);

        // Draw center dot
        paint.setColor(color);
        canvas.drawCircle(radius, radius, radius/4, paint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }


    public void updateWifiPoints(List<WifiPoint> wifiPoints) {
        clearMap();
        wifiPointMap.clear();

        for (WifiPoint point : wifiPoints) {
            if (!point.hasValidPosition()) continue;

            // Create small dot for WiFi point
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(point.getPosition().toLatLng())
                    .title(point.getSsid())
                    .icon(createDotMarker(WIFI_DOT_SIZE, Color.BLUE))
                    .anchor(0.5f, 0.5f)
                    .zIndex(1.0f);

            Marker marker = map.addMarker(markerOptions);
            wifiMarkers.add(marker);

            // Create signal range circle
            CircleOptions circleOptions = new CircleOptions()
                    .center(point.getPosition().toLatLng())
                    .radius(point.getDistance())
                    .strokeWidth(4) // thicker border
                    .strokeColor(Color.parseColor(point.getSignalColor()))
                    .fillColor(Color.TRANSPARENT);

            Circle circle = map.addCircle(circleOptions);
            wifiCircles.add(circle);
            wifiPointMap.put(marker.getId(), point);
        }
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


    private void updateIntersectionArea(List<WifiPoint> wifiPoints) {
        if (intersectionArea != null) {
            intersectionArea.remove();
        }

        WifiTriangulation triangulation = new WifiTriangulation(wifiPoints);
        List<LatLng> intersectionPoints = calculateIntersectionPolygon(triangulation);

        if (!intersectionPoints.isEmpty()) {
            try {
                PolygonOptions polygonOptions = new PolygonOptions()
                        .addAll(intersectionPoints)
                        .strokeWidth(2)
                        .strokeColor(Color.BLUE)
                        .fillColor(Color.argb(70, 0, 0, 255)); // More visible blue with transparency

                intersectionArea = map.addPolygon(polygonOptions);
            } catch (Exception e) {
                Log.e("MapManager", "Error creating polygon: " + e.getMessage());
            }
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
