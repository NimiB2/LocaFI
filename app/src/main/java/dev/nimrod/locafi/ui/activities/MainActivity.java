package dev.nimrod.locafi.ui.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
import dev.nimrod.locafi.ui.adapters.WifiListAdapter;
import dev.nimrod.locafi.ui.views.LocationView;
import dev.nimrod.locafi.ui.views.MapFragment;

public class MainActivity extends AppCompatActivity{
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";
    private FusedLocationProviderClient fusedLocationClient;

    private MapFragment mapFragment;


    // UI Components
    private RecyclerView wifiListRecyclerView;
    private MaterialButton scanButton;
    private MaterialButton manageButton;
    private FrameLayout visualizationContainer;
    private WifiListAdapter wifiListAdapter;

    // Utilities
    private WifiManager wifiManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeServices();
        setupMapFragment();
        showDisclaimerDialog();
        setupListeners();
        setupRecyclerView();
        checkPermissions();

        if (!checkGooglePlayServices()) {
            Log.e("MainActivity", "Google Play Services not available");
            return;
        }
    }

    private void initializeServices() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    private void initializeViews() {
        // Initialize toolbar
        MaterialToolbar toolbar = findViewById(R.id.main_MTB_toolbar);
        setSupportActionBar(toolbar);

        // Initialize other views
        wifiListRecyclerView = findViewById(R.id.main_RCV_wifiList);
        scanButton = findViewById(R.id.main_BTN_scan);          // Initialize buttons first
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
            Log.d("MainActivity", "Map is ready");
            // First get location, then start WiFi scan
            if (checkLocationPermission()) {
                startWifiScan();
            }
        });
    }

    private boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return false;
        }
        return true;
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

    private void setupListeners() {
        // Scan button listener
        scanButton.setOnClickListener(v -> startWifiScan());

        // Manage button listener
        manageButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, WifiManagement.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        wifiListAdapter = new WifiListAdapter(new ArrayList<>());
        wifiListRecyclerView.setAdapter(wifiListAdapter);
        wifiListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
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

    private void startWifiScan() {
        if (!wifiManager.isWifiEnabled()) {
            showEnableWifiDialog();
            return;
        }

        boolean success = wifiManager.startScan();
        if (!success) {
            Toast.makeText(this, "Failed to start Wi-Fi scan", Toast.LENGTH_SHORT).show();
        }
    }

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            if (success) {
                scanSuccess();
            } else {
                scanFailure();
            }
        }
    };

    private void scanSuccess() {
        List<ScanResult> results = getScanResults();
        List<ScanResult> filteredResults = filterAndSortResults(results);
        updateVisibility(!filteredResults.isEmpty());
        wifiListAdapter.updateData(filteredResults);

        // Convert ScanResults to WifiPoints and update map
        List<WifiPoint> wifiPoints = convertToWifiPoints(filteredResults);
        mapFragment.updateWifiPoints(wifiPoints);
    }


    private void scanFailure() {
        List<ScanResult> results = getScanResults();

        if (results.isEmpty()) {
            updateVisibility(false); // Show empty states
            Toast.makeText(this, "No WiFi networks found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ScanResult> filteredResults = filterAndSortResults(results);
        updateVisibility(!filteredResults.isEmpty());
        wifiListAdapter.updateData(filteredResults);
        updateVisualization(filteredResults);

        Snackbar.make(
                visualizationContainer,
                "Showing cached WiFi scan results",
                Snackbar.LENGTH_LONG
        ).setAction("Retry", v -> startWifiScan()).show();
    }

    private List<WifiPoint> convertToWifiPoints(List<ScanResult> scanResults) {
        List<WifiPoint> wifiPoints = new ArrayList<>();
        for (ScanResult result : scanResults) {
            WifiPosition position = calculateWifiPosition(result);
            double distance = calculateDistance(result.level);

            WifiPoint wifiPoint = new WifiPoint(
                    result.SSID,
                    result.BSSID,
                    result.level,
                    distance,
                    position
            );
            wifiPoints.add(wifiPoint);
        }
        return wifiPoints;
    }

    private WifiPosition calculateWifiPosition(ScanResult result) {
        Location location = mapFragment.getLastKnownLocation();
        if (location == null) {
            return null;
        }

        double distance = calculateDistance(result.level);
        // Use signal strength to determine approximate direction instead of random
        double bearing = (result.level + 100) * 3.6; // Convert signal strength to angle (0-360)

        return new WifiPosition(location.getLatitude(), location.getLongitude())
                .calculateDestination(distance, bearing);
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

    private void requestNewLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setNumUpdates(1)
                .setInterval(0);

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    mapFragment.updateUserLocation(location);
                }
            }
        }, Looper.getMainLooper());
    }

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                PERMISSION_REQUEST_CODE);
    }

    @SuppressLint("MissingPermission")
    private List<ScanResult> getScanResults() {
        try {
            if (checkScanPermission()) {
                return wifiManager.getScanResults();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission error while scanning WiFi", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error while scanning WiFi", Toast.LENGTH_SHORT).show();
        }
        return new ArrayList<>();
    }

    private boolean checkScanPermission() {
        // For Android 10 (API 29) and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                    == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
                            == PackageManager.PERMISSION_GRANTED;
        } else {
            // For older versions
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private List<ScanResult> filterAndSortResults(List<ScanResult> results) {
        return results.stream()
                .filter(result -> result.SSID != null && !result.SSID.isEmpty())
                .sorted((r1, r2) -> Integer.compare(r2.level, r1.level))
                .collect(Collectors.toList());
    }

    private void updateVisibility(boolean hasData) {
        // Visualization
        findViewById(R.id.main_LLC_empty_visualization).setVisibility(hasData ? View.GONE : View.VISIBLE);
        findViewById(R.id.main_VIS_location).setVisibility(hasData ? View.VISIBLE : View.GONE);

        // WiFi List
        findViewById(R.id.main_LLC_empty_list).setVisibility(hasData ? View.GONE : View.VISIBLE);
        findViewById(R.id.main_RCV_wifiList).setVisibility(hasData ? View.VISIBLE : View.GONE);
    }

    private void updateVisualization(List<ScanResult> results) {
        List<ScanResult> filteredResults = filterAndSortResults(results);
        updateVisibility(!filteredResults.isEmpty());
        wifiListAdapter.updateData(filteredResults);

        // Convert to WiFi points and update map
        List<WifiPoint> wifiPoints = convertToWifiPoints(filteredResults);
        mapFragment.updateWifiPoints(wifiPoints);

        // When updating visualization, preserve user location
        mapFragment.zoomToFitWifiPoints(wifiPoints, true);
    }

    private double calculateDistance(int rssi) {
        // Calculate approximate distance using the log-distance path loss model
        double referenceDistance = 1.0; // 1 meter
        double referenceRSSI = -40.0;   // RSSI at reference distance
        double pathLossExponent = 2.7;  // Path loss exponent

        return referenceDistance * Math.pow(10.0,
                (referenceRSSI - rssi) / (10.0 * pathLossExponent));
    }

    private void showEnableWifiDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Wi-Fi Required")
                .setMessage("Please enable Wi-Fi to scan for networks")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDisclaimerDialog() {

//        new MaterialAlertDialogBuilder(this)
//                .setTitle("apiKey")
//                .setMessage("This app demonstrates how device position can be " +
//                        "approximated using only WiFi signals and signal strength measurements. " +
//                        "This highlights potential privacy implications of WiFi scanning capabilities.")
//                .setPositiveButton("Understand", null)
//                .show();
    }

    private boolean shouldShowPermissionRationale() {
        return ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_WIFI_STATE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CHANGE_WIFI_STATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiScanReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    Arrays.stream(grantResults).allMatch(result -> result == PackageManager.PERMISSION_GRANTED)) {
                // All permissions granted, proceed with functionality
                startWifiScan();
            } else {
                // Show explanation and provide way to request again
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Permissions Required")
                        .setMessage("WiFi permissions are required for scanning networks and determining " +
                                "approximate position. Please grant these permissions to continue.")
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show();
                        })
                        .show();
            }
        }
    }
}