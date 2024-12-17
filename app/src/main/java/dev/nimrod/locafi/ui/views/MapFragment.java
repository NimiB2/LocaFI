package dev.nimrod.locafi.ui.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.ui.maps.MapManager;

public class MapFragment extends Fragment {
    private GoogleMap googleMap;
    private Runnable onMapReadyCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton fabMyLocation;
    private Location lastKnownLocation;
    private MapManager mapManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        fabMyLocation = view.findViewById(R.id.fab_my_location);
        fabMyLocation.setOnClickListener(v -> getCurrentLocation());

        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.wifi_map);

        supportMapFragment.getMapAsync(googleMap -> {
            this.googleMap = googleMap;
            this.mapManager = new MapManager(googleMap, requireContext());
            if (onMapReadyCallback != null) {
                onMapReadyCallback.run();
                onMapReadyCallback = null;
            }
            getCurrentLocation();
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        return view;
    }

    public void setOnMapReadyCallback(Runnable callback) {
        if (googleMap != null) {
            callback.run();
        } else {
            onMapReadyCallback = callback;
        }
    }

    private void setupMap() {
        if (googleMap == null) return;

        try {
            // Enable zoom controls
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setZoomGesturesEnabled(true);

            // Set default location (e.g., Tel Aviv)
            LatLng defaultLocation = new LatLng(32.0853, 34.7818);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15));

            // Add a marker to verify map is working
            googleMap.addMarker(new MarkerOptions()
                    .position(defaultLocation)
                    .title("Default Location"));

            // Set map type
            googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

            Log.d("MapFragment", "Map setup completed successfully");
        } catch (Exception e) {
            Log.e("MapFragment", "Error setting up map: " + e.getMessage());
        }
    }

    public void updateWifiPoints(List<WifiPoint> wifiPoints) {
        if (googleMap == null) {
            setOnMapReadyCallback(() -> updateWifiPoints(wifiPoints));
            return;
        }
        if (mapManager != null) {
            mapManager.updateWifiPoints(wifiPoints);
        }
    }

    public void updateUserLocation(Location location) {
        if (googleMap == null) {
            setOnMapReadyCallback(() -> updateUserLocation(location));
            return;
        }
        if (mapManager != null && location != null) {
            lastKnownLocation = location;
            mapManager.updateUserLocation(location);
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), this::updateUserLocation);
    }

    public void clearMap() {
        if (googleMap == null) {
            setOnMapReadyCallback(this::clearMap);
            return;
        }
        if (mapManager != null) {
            mapManager.clearMap();
        }
    }

    public Location getLastKnownLocation() {
        return lastKnownLocation;
    }

    public void zoom(double lat, double lon, String title) {
        if (googleMap == null) {
            setOnMapReadyCallback(() -> zoom(lat, lon, title));
            return;
        }
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lon))
                .title(title));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(lat, lon))
                .zoom(15)
                .build();
        googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    // Zoom to show all WiFi points
    public void zoomToFitWifiPoints(List<WifiPoint> wifiPoints) {
        if (googleMap == null) {
            setOnMapReadyCallback(() -> zoomToFitWifiPoints(wifiPoints));
            return;
        }

        if (wifiPoints == null || wifiPoints.isEmpty()) {
            return;
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (WifiPoint point : wifiPoints) {
            if (point.hasValidPosition()) {
                builder.include(new LatLng(point.getLatitude(), point.getLongitude()));
            }
        }

        try {
            LatLngBounds bounds = builder.build();
            int padding = 100; // offset from edges of the map in pixels
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
            googleMap.animateCamera(cu);
        } catch (Exception e) {
            // Fallback to default zoom if bounds calculation fails
            if (!wifiPoints.isEmpty()) {
                WifiPoint firstPoint = wifiPoints.get(0);
                zoom(firstPoint.getLatitude(), firstPoint.getLongitude(), firstPoint.getSsid());
            }
        }
    }
}