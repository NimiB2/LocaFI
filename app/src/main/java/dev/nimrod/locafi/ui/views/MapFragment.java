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
    private GoogleMap googleMap;
    private Runnable onMapReadyCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private MapManager mapManager;
    private boolean isInitialLocationSet = false;

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
            getCurrentLocation(true);
            if (onMapReadyCallback != null) {
                onMapReadyCallback.run();
                onMapReadyCallback = null;
            }
        });

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

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setZoomGesturesEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Disable the My Location button
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Safely disable the location layer with permission check
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(false);
        }

        googleMap.setMinZoomPreference(3.0f);
        googleMap.setMaxZoomPreference(22.0f);

        googleMap.setMyLocationEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
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
                    .zoom(17)  // Closer zoom level
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
            setOnMapReadyCallback(() -> updateWifiPoints(wifiPoints));
            return;
        }

        if (mapManager != null && !wifiPoints.isEmpty()) {
            mapManager.updateWifiPoints(wifiPoints);

            // Add logging to verify location updates
            List<LocationCalculator.WeightedLocation> locations =
                    LocationCalculator.calculatePossibleLocations(wifiPoints);
            if (!locations.isEmpty()) {
                LocationCalculator.WeightedLocation location = locations.get(0);
                Log.d("MapFragment", "Calculated WiFi Location: " +
                        location.location.latitude + ", " + location.location.longitude);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(new LocationCallback(){});
        }
    }
}