package dev.nimrod.locafi.ui.activities;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;

import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import android.Manifest;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.services.MapDataService;
import dev.nimrod.locafi.ui.adapters.WifiListAdapter;
import dev.nimrod.locafi.ui.views.MapFragment;
import dev.nimrod.locafi.utils.LocationPermissionHandler;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FusedLocationProviderClient fusedLocationClient;
    private boolean initialPositionSet = false;
    private LocationCallback locationCallback;
    private LocationPermissionHandler permissionHandler;


    private MapFragment mapFragment;
    private MapDataService mapService;
    private ServiceConnection serviceConnection;

    // UI Components
    private RecyclerView wifiListRecyclerView;

    private FrameLayout visualizationContainer;
    private WifiListAdapter wifiListAdapter;
    private MaterialButton main_BTN_location;
    private boolean isComparisonMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        permissionHandler = new LocationPermissionHandler(this, new LocationPermissionHandler.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                initializeApp();
            }
        });
        permissionHandler.requestLocationPermission();
    }






    private void initializeApp() {
        // Check for location services first
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showLocationServicesDialog();
            return;
        }

        initializeServiceConnection();
        initializeViews();
        initializeServices();
        setupMapFragment();
        setupListeners();
        setupRecyclerView();
    }

    private void showLocationServicesDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable Location Services")
                .setMessage("Location services are required for this app to work properly.")
                .setPositiveButton("Settings", (dialog, which) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Cancel", (dialog, which) -> finishAffinity())
                .show();
    }


    private void initializeServiceConnection() {
        Intent serviceIntent = new Intent(this, MapDataService.class);
        startService(serviceIntent);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MapDataService.LocalBinder binder = (MapDataService.LocalBinder) service;
                mapService = binder.getService();
                setupMapDataObservers();  // Always setup observers first

                // Now check if we need to initialize location
                if (!mapService.hasBaseLocation()) {
                    initializeLocation();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mapService = null;
            }
        };

        bindService(new Intent(this, MapDataService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void resetComparisonMode() {
        isComparisonMode = false;
        updateComparisonUI();
    }

    private void updateComparisonUI() {
        // Update map fragment
        if (mapFragment != null) {
            mapFragment.toggleLocationComparison(isComparisonMode);
        }
    }

    private void initializeViews() {
        // Initialize toolbar
        MaterialToolbar toolbar = findViewById(R.id.main_MTB_toolbar);
        setSupportActionBar(toolbar);

        // Initialize other views
        wifiListRecyclerView = findViewById(R.id.main_RCV_wifiList);

        main_BTN_location = findViewById(R.id.main_BTN_location);
        visualizationContainer = findViewById(R.id.main_VIS_location);

        // Initialize empty states visibility
        visualizationContainer.setVisibility(View.GONE);
        findViewById(R.id.main_LLC_empty_list).setVisibility(View.VISIBLE);
        wifiListRecyclerView.setVisibility(View.GONE);
        findViewById(R.id.main_VIS_location).setVisibility(View.VISIBLE);
        // Initialize location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initializeServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    private void initializeLocation() {
        Log.d(TAG, "Initializing location updates");
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null && mapService != null) {
                    if (location.getAccuracy() > 50) { // 50 meters threshold
                        showInaccurateLocationDialog();
                    }
                    mapService.updateBaseLocation(location);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Error requesting location updates: " + e.getMessage());
        }
    }


    private void showInaccurateLocationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Improve Location Accuracy")
                .setMessage("Your current location accuracy is low. For better results:\n" +
                        "• Enable GPS/High accuracy mode\n" +
                        "• Move to an open area\n" +
                        "• Wait a few moments for better signal")
                .setPositiveButton("Open Settings", (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Continue Anyway", null)
                .show();
    }

    private void requestNewLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000);  // Update every 10 seconds

        fusedLocationClient.requestLocationUpdates(locationRequest,
                new LocationCallback() {
                    @Override
                    public void onLocationResult(@NonNull LocationResult locationResult) {
                        Location location = locationResult.getLastLocation();
                        if (location != null && mapService != null) {
                            mapService.updateBaseLocation(location);
                        }
                    }
                },
                Looper.getMainLooper());
    }


    private void setupMapFragment() {
        // Use existing instance if possible
        mapFragment = (MapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.main_VIS_location);

        if (mapFragment == null) {
            mapFragment = new MapFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_VIS_location, mapFragment)
                    .commit();
        }

        mapFragment.setOnMapReadyCallback(() -> {
            Log.d(TAG, "Map is ready");
            // Initialize location immediately when map is ready
            if (mapService != null) {
                mapService.preInitializeMap();
                if (!mapService.hasBaseLocation()) {
                    initializeLocation();
                }
            }
        });
    }

    private void setupMapDataObservers() {
        mapService.getWifiPointsData().observe(this, wifiPoints -> {
            Log.d(TAG, "Received " + (wifiPoints != null ? wifiPoints.size() : 0) + " WiFi points");

            // Update map
            if (mapFragment != null) {
                mapFragment.updateWifiPoints(wifiPoints);

                // Set initial position only once using first WiFi point
                if (wifiPoints != null && !wifiPoints.isEmpty() && !initialPositionSet) {
                    WifiPoint firstPoint = wifiPoints.get(0);
                    if (firstPoint.hasValidPosition()) {
                        mapFragment.setInitialPosition(new LatLng(firstPoint.getLatitude(),
                                firstPoint.getLongitude()), 19f);
                        initialPositionSet = true;
                    }
                }
            }

            if (wifiPoints != null && !wifiPoints.isEmpty()) {
                wifiListAdapter.updateData(wifiPoints);
                updateWifiListVisibility(true);
            } else {
                updateWifiListVisibility(false);
            }

            findViewById(R.id.main_PGI_loading).setVisibility(View.GONE);
        });

        mapService.getIsMapReady().observe(this, isReady -> {
            if (isReady) {
                findViewById(R.id.main_PGI_loading).setVisibility(View.GONE);
            }
        });
    }

    private void updateWifiListVisibility(boolean hasWifiPoints) {
        findViewById(R.id.main_LLC_empty_list).setVisibility(
                hasWifiPoints ? View.GONE : View.VISIBLE);
        wifiListRecyclerView.setVisibility(
                hasWifiPoints ? View.VISIBLE : View.GONE);
    }


    private void setupListeners() {
        main_BTN_location.setOnClickListener(v -> {
            isComparisonMode = !isComparisonMode;
            updateComparisonMode();
        });
    }

    private void updateComparisonMode() {
        // Update button background color
        main_BTN_location.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(
                        isComparisonMode ? R.color.secondary_color : R.color.primary_color,
                        null
                )
        ));

        // Update button text and text color
        main_BTN_location.setText(isComparisonMode ?
                R.string.hide_gps_location : R.string.show_gps_location);
        main_BTN_location.setTextColor(getResources().getColor(
                isComparisonMode ? R.color.primary_color : R.color.secondary_color,
                null
        ));

        // Update map fragment
        if (mapFragment != null) {
            mapFragment.toggleLocationComparison(isComparisonMode);
        }
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("comparison_mode", isComparisonMode);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isComparisonMode = savedInstanceState.getBoolean("comparison_mode", false);
        updateComparisonMode();
    }


    private void setupRecyclerView() {
        wifiListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        wifiListAdapter = new WifiListAdapter(new ArrayList<>());
        wifiListAdapter.setOnWifiPointClickListener(wifiPoint -> {
            // Find corresponding WifiPoint
            List<WifiPoint> currentPoints = mapService.getWifiPointsData().getValue();
            if (currentPoints != null) {
                for (WifiPoint point : currentPoints) {
                    if (point.getBssid().equals(wifiPoint.getBssid())) {
                        mapFragment.focusOnWifiPoint(point);
                        break;
                    }
                }
            }
        });
        wifiListRecyclerView.setAdapter(wifiListAdapter);
    }


    private boolean checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 2404).show();
            }
            return false;
        }
        return true;
    }

    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        // Using a CompletableFuture to handle the async nature of getLastLocation
        CompletableFuture<Location> locationFuture = new CompletableFuture<>();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, locationFuture::complete)
                .addOnFailureListener(e -> locationFuture.complete(null));

        try {
            Location location = locationFuture.get(1, TimeUnit.SECONDS);
            if (location == null) {
                // If last location is null, try getting current location
                requestNewLocation();
            }
            return location;
        } catch (Exception e) {
            return null;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mapService != null && !mapService.hasBaseLocation()) {
            initializeLocation();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (mapFragment != null) {
            mapFragment.cleanup();
        }
        if (serviceConnection != null && mapService != null) {
            unbindService(serviceConnection);
            serviceConnection = null;
            mapService = null;
        }
    }
}