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
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.LocationCalculator;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.models.WifiTriangulation;
import dev.nimrod.locafi.ui.maps.MapManager;

public class MapFragment extends Fragment {

    private static final String TAG = "MapFragment";
    private GoogleMap googleMap;
    private Runnable onMapReadyCallback;
    private MapManager mapManager;
    private boolean isInitialLocationSet = false;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private boolean isMapInitialized = false;

    private FloatingActionButton locationFAB;
    private LatLng currentWifiLocation;
    private boolean userInteracting = false;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment supportMapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.wifi_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());


        supportMapFragment.getMapAsync(googleMap -> {
            this.googleMap = googleMap;
            this.mapManager = new MapManager(googleMap, requireContext());
            setupMap();
            isMapInitialized = true;

            if (onMapReadyCallback != null) {
                onMapReadyCallback.run();
                onMapReadyCallback = null;
            }
        });

        initializeViews(view);

        return view;
    }

    private void initializeViews(View view) {
        locationFAB = view.findViewById(R.id.map_FAB_location);
        locationFAB.setOnClickListener(v -> focusOnWifiLocation());
    }
    private void focusOnWifiLocation() {
        if (currentWifiLocation != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currentWifiLocation)
                    .zoom(40f)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public boolean isMapReady() {
        return isMapInitialized;
    }

    public void focusOnWifiPoint(WifiPoint point) {
        if (googleMap != null && point.hasValidPosition()) {
            LatLng position = new LatLng(point.getLatitude(), point.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(position)
                    .zoom(40f)  // Very close zoom for specific point
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }
    public void updateWifiPointsWithoutCamera(List<WifiPoint> wifiPoints) {
        if (googleMap == null) {
            Log.d(TAG, "Map not ready, queuing update");
            setOnMapReadyCallback(() -> updateWifiPointsWithoutCamera(wifiPoints));
            return;
        }

        Log.d(TAG, "Updating WiFi points: " + wifiPoints.size());

        if (mapManager != null && !wifiPoints.isEmpty()) {
            mapManager.updateWifiPoints(wifiPoints);
        }
    }
    public void updateWifiLocation(LatLng location) {
        if (mapManager != null) {
            mapManager.updateWifiBasedLocation(location);
            // Store for FAB usage
            this.currentWifiLocation = location;
        }
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

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(true);
        //googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        // Safely disable the location layer with permission check
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(false);
        }

        googleMap.setMinZoomPreference(2.0f);
        googleMap.setMaxZoomPreference(100.0f);

        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                userInteracting = true;
            }
        });


    }

    private void getCurrentLocation(boolean isInitial) {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        lastKnownLocation = location;
                        updateUserLocation(location);
                        if (isInitial && !isInitialLocationSet) {
                            focusOnUserLocation();
                            isInitialLocationSet = true;
                        }
                    }
                });
    }

    private void focusOnUserLocation() {
        if (lastKnownLocation != null && googleMap != null) {
            LatLng userLatLng = new LatLng(lastKnownLocation.getLatitude(),
                    lastKnownLocation.getLongitude());

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(userLatLng)
                    .zoom(40)
                    .build();

            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),
                    1000, null);  // 1 second animation
        }
    }

    public void updateUserLocation(Location location) {
        // Comment out or remove the marker creation/update code
    /* if (userMarker == null) {
        MarkerOptions markerOptions = new MarkerOptions()
            .position(userLatLng)
            .title("Real GPS Location")
            .icon(getBitmapDescriptorFromVector(R.drawable.gps_location_icon));
        userMarker = map.addMarker(markerOptions);
    } else {
        userMarker.setPosition(userLatLng);
    } */
    }

    // Override the zoomToFitWifiPoints method to preserve camera position when needed
    public void zoomToFitWifiPoints(List<WifiPoint> wifiPoints, boolean preserveUserLocation) {
        if (googleMap == null || wifiPoints == null || wifiPoints.isEmpty()) {
            return;
        }

        if (preserveUserLocation && lastKnownLocation != null) {
            focusOnUserLocation();
            return;
        }

        // Original zooming logic for WiFi points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (WifiPoint point : wifiPoints) {
            if (point.hasValidPosition()) {
                builder.include(new LatLng(point.getLatitude(), point.getLongitude()));
            }
        }

        try {
            LatLngBounds bounds = builder.build();
            int padding = 100;
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (Exception e) {
            Log.e("MapFragment", "Error zooming to fit points: " + e.getMessage());
        }
    }

    public Location getLastKnownLocation() {
        if (googleMap == null) {
            Log.e("MapFragment", "Google Map is not ready yet.");
            return null;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("MapFragment", "Location permission not granted.");
            return null;
        }

        // Enable the "My Location" layer on the map
        googleMap.setMyLocationEnabled(true);

        // Get last known location directly from GoogleMap
        return googleMap.getMyLocation();
    }



    public void updateWifiPoints(List<WifiPoint> wifiPoints) {
        if (googleMap == null) {
            Log.d(TAG, "Map not ready, queuing update");
            setOnMapReadyCallback(() -> updateWifiPoints(wifiPoints));
            return;
        }

        Log.d(TAG, "Updating WiFi points: " + wifiPoints.size());

        if (mapManager != null && !wifiPoints.isEmpty()) {
            mapManager.updateWifiPoints(wifiPoints);

            if (!isInitialLocationSet) {
                WifiPoint firstPoint = wifiPoints.get(0);
                if (firstPoint.hasValidPosition()) {
                    moveCameraToPosition(new LatLng(firstPoint.getLatitude(), firstPoint.getLongitude()));
                    isInitialLocationSet = true;
                }
            }
        }
    }

    public void onCameraIdle() {
        userInteracting = true;
    }
    private void moveCameraToPosition(LatLng position) {
        if (googleMap == null || position == null) return;

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(position)
                .zoom(40f)
                .build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition),
                1000, null);

        Log.d(TAG, "Camera moved to: " + position.latitude + ", " + position.longitude);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(new LocationCallback(){});
        }
        mapManager = null;
    }
}