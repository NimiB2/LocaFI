package dev.nimrod.locafi.ui.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import android.Manifest;

import dev.nimrod.locafi.BuildConfig;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.models.WifiPosition;
import dev.nimrod.locafi.services.MapDataService;
import dev.nimrod.locafi.ui.adapters.WifiListAdapter;
import dev.nimrod.locafi.ui.views.LocationView;
import dev.nimrod.locafi.ui.views.MapFragment;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private FusedLocationProviderClient fusedLocationClient;

    private MapFragment mapFragment;
    private MapDataService mapService;
    private ServiceConnection serviceConnection;

    // UI Components
    private RecyclerView wifiListRecyclerView;
    private MaterialButton scanButton;
    private MaterialButton manageButton;
    private FrameLayout visualizationContainer;
    private WifiListAdapter wifiListAdapter;

    // Utilities


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeServiceConnection();
        initializeViews();
        initializeServices();
        setupMapFragment();
        setupListeners();
        setupRecyclerView();
        checkPermissions();

        if (!checkGooglePlayServices()) {
            Log.e("MainActivity", "Google Play Services not available");
            return;
        }
    }

    private void initializeServiceConnection() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MapDataService.LocalBinder binder = (MapDataService.LocalBinder) service;
                mapService = binder.getService();
                setupMapDataObservers();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mapService = null;
            }
        };

        // Bind to the service
        bindService(new Intent(this, MapDataService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void initializeServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    private void initializeLocation() {
        if (checkLocationPermission()) {
            requestNewLocation();
        }
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

    private void initializeViews() {
        // Initialize toolbar
        MaterialToolbar toolbar = findViewById(R.id.main_MTB_toolbar);
        setSupportActionBar(toolbar);

        // Initialize other views
        wifiListRecyclerView = findViewById(R.id.main_RCV_wifiList);
        scanButton = findViewById(R.id.main_BTN_scan);
        manageButton = findViewById(R.id.main_BTN_manage);
        visualizationContainer = findViewById(R.id.main_VIS_location);

        // Initialize empty states visibility
        findViewById(R.id.main_LLC_empty_visualization).setVisibility(View.VISIBLE);
        visualizationContainer.setVisibility(View.GONE);
        findViewById(R.id.main_LLC_empty_list).setVisibility(View.VISIBLE);
        wifiListRecyclerView.setVisibility(View.GONE);
    }

    private void setupMapFragment() {
        mapFragment = new MapFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_VIS_location, mapFragment)
                .commit();

        mapFragment.setOnMapReadyCallback(() -> {
            Log.d(TAG, "Map is ready");
            // Initialize location immediately when map is ready
            initializeLocation();
        });
    }

    private void setupMapDataObservers() {
        mapService.getWifiPointsData().observe(this, wifiPoints -> {
            Log.d(TAG, "Received " + (wifiPoints != null ? wifiPoints.size() : 0) + " WiFi points");

            if (mapFragment != null) {
                mapFragment.updateWifiPoints(wifiPoints);
            }
        });

        mapService.getUserLocationData().observe(this, location -> {
            Log.d(TAG, "Received user location update: " +
                    (location != null ? location.getLatitude() + ", " + location.getLongitude() : "null"));
            if (mapFragment != null) {
                mapFragment.updateUserLocation(location);
            }
        });

        mapService.getIsMapReady().observe(this, isReady -> {
            findViewById(R.id.main_LLC_empty_visualization).setVisibility(
                    isReady ? View.GONE : View.VISIBLE);
            findViewById(R.id.main_VIS_location).setVisibility(
                    isReady ? View.VISIBLE : View.GONE);
        });
    }

    private void setupListeners() {
        // Add scan button listener
        scanButton.setOnClickListener(v -> {
            if (mapService != null) {
                // Show loading indicator or progress
                scanButton.setEnabled(false);

                // Trigger location update first
                if (checkLocationPermission()) {
                    requestNewLocation();
                }

                // Start WiFi scan - the service will handle the scanning process
                mapService.performWifiScan();

                // Re-enable button after a delay (e.g., 2 seconds)
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    scanButton.setEnabled(true);
                }, 2000);
            } else {
                Toast.makeText(this, "Service not ready", Toast.LENGTH_SHORT).show();
            }
        });

        // Existing manage button listener
        manageButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WifiManagement.class);
            startActivity(intent);
        });
    }
    private void testPermissions() {
        Log.d(TAG, "Testing Permissions:");
        Log.d(TAG, "WiFi State Permission: " +
                checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE));
        Log.d(TAG, "Location Permission: " +
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    }
    private void setupRecyclerView() {
        wifiListAdapter = new WifiListAdapter(new ArrayList<>());
        wifiListRecyclerView.setAdapter(wifiListAdapter);
        wifiListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
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

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return false;
        }
        return true;
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
        Log.d(TAG, "MainActivity initialized");
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestLocationPermissions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Location Permission Required")
                .setMessage("LocaFI needs precise location access to accurately calculate WiFi positions and provide better triangulation results. This helps improve the accuracy of WiFi-based positioning.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    ActivityCompat.requestPermissions(this,
                            new String[]{
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                            },
                            PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private boolean shouldShowPermissionRationale() {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_WIFI_STATE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CHANGE_WIFI_STATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean hasPreciseLocation = false;
            boolean hasCoarseLocation = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    hasPreciseLocation = true;
                }
                if (permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION)
                        && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    hasCoarseLocation = true;
                }
            }

            if (hasPreciseLocation) {
                // Best case - proceed with full functionality
                mapService.startWithPreciseLocation();
            } else if (hasCoarseLocation) {
                // Limited functionality - notify user
                Snackbar.make(findViewById(android.R.id.content),
                        "Using approximate location. Accuracy may be reduced.",
                        Snackbar.LENGTH_LONG).show();
                mapService.startWithApproximateLocation();
            } else {
                // No location permission - show settings dialog
                showLocationSettingsDialog();
            }
        }
    }

    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
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


    private void showLocationSettingsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Location Permission Required")
                .setMessage("WiFi positioning requires location access. Without it, the app cannot function properly. Please grant location permission in Settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "App functionality will be limited without location access",
                            Toast.LENGTH_LONG).show();
                })
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null) {
            unbindService(serviceConnection);
        }
    }
}