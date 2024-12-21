package dev.nimrod.locafi.ui.views;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.LocationCalculator;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.models.WifiTriangulation;
import dev.nimrod.locafi.ui.activities.MainActivity;
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

    private boolean isComparisonMode = false;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private boolean userHasInteracted = false;
    private Location currentGpsLocation;

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
        locationFAB.setOnClickListener(v -> {
            userHasInteracted = false;  // Reset interaction flag
            focusOnWifiLocation();
        });
    }
    private void focusOnWifiLocation() {
        if (currentWifiLocation != null) {
            userHasInteracted = false;  // Reset user interaction flag
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(currentWifiLocation)
                    .zoom(19f)
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), new GoogleMap.CancelableCallback() {
                @Override
                public void onFinish() {
                    Log.d(TAG, "Camera animation finished - focused on WiFi location");
                }

                @Override
                public void onCancel() {
                    Log.d(TAG, "Camera animation canceled");
                }
            });
        } else {
            Log.d(TAG, "Cannot focus - currentWifiLocation is null");
        }
    }
    public void focusOnWifiPoint(WifiPoint point) {
        if (googleMap != null && point.hasValidPosition()) {
            userHasInteracted = false;
            LatLng position = new LatLng(point.getLatitude(), point.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(position)
                    .zoom(19f)  // Increased zoom level
                    .build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public boolean isMapReady() {
        return isMapInitialized;
    }


    public void toggleLocationComparison(boolean enabled) {
        isComparisonMode = enabled;
        if (enabled && !checkLocationServices()) {
            isComparisonMode = false;
            return;
        }

        if (mapManager != null) {
            mapManager.toggleLocationComparison(enabled);
            if (enabled) {
                startLocationUpdates();
                userHasInteracted = false;  // Reset interaction when entering comparison mode
            } else {
                stopLocationUpdates();
            }
        }
    }

    public void cleanup() {
        userHasInteracted = false;
        isInitialLocationSet = false;
        if (mapManager != null) {
            mapManager.cleanup();
        }
    }

    private boolean checkLocationServices() {
        // Check if location is enabled in device settings
        LocationManager locationManager = (LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showEnableLocationDialog();
            return false;
        }

        // Check permissions
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return false;
        }

        return true;
    }

    private void showEnableLocationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Location Services Required")
                .setMessage("GPS location is needed for comparison. Would you like to enable it?")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    notifyComparisonCancelled();
                })
                .show();
    }

    private void requestLocationPermission() {
        requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST
        );
    }

    private void notifyComparisonCancelled() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).resetComparisonMode();
        }
    }
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000); // Update every 5 seconds

        fusedLocationClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        if (location != null && isComparisonMode) {
                            currentGpsLocation = location;
                            mapManager.updateComparisonVisualization(location);
                        }
                    }
                },
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(new LocationCallback() {});
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
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // Always keep Google's location marker disabled
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(false);
        }

        googleMap.setMinZoomPreference(2.0f);
        googleMap.setMaxZoomPreference(21f); // Increased max zoom level

        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                userHasInteracted = true;
            }
        });
    }
    public void resetUserInteraction() {
        userHasInteracted = false;
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
        Log.d(TAG, "User has interacted with map: " + userHasInteracted);

        if (mapManager != null && !wifiPoints.isEmpty()) {
            mapManager.updateWifiPoints(wifiPoints);

            // Update current WiFi location based on first point
            if (wifiPoints.get(0).hasValidPosition()) {
                currentWifiLocation = new LatLng(
                        wifiPoints.get(0).getLatitude(),
                        wifiPoints.get(0).getLongitude()
                );
                Log.d(TAG, "Updated currentWifiLocation to: " + currentWifiLocation.latitude + ", " + currentWifiLocation.longitude);
            }
        }
    }

    public void setInitialPosition(LatLng position, float zoom) {
        if (googleMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(position)
                    .zoom(zoom)
                    .build();
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else {
            setOnMapReadyCallback(() -> {
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(position)
                        .zoom(zoom)
                        .build();
                googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            });
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(new LocationCallback(){});
        }
        mapManager = null;
    }
}