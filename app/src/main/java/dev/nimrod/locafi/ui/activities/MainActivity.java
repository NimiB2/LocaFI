package dev.nimrod.locafi.ui.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.nimrod.locafi.Manifest;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.ui.adapters.WifiListAdapter;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "MainActivity";

    // UI Components
    private RecyclerView wifiListRecyclerView;
    private MaterialButton scanButton;
    private MaterialButton manageButton;
    private FrameLayout visualizationContainer;
    private WifiListAdapter wifiListAdapter;

    // Utilities
    private WifiManager wifiManager;
    private LocationManager locationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeServices();
        initializeViews();
        setupListeners();
        setupRecyclerView();
        checkPermissions();
    }

    private void initializeServices() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
                Manifest.permission.ACCESS_FINE_LOCATION,
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

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showEnableLocationDialog();
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
        List<ScanResult> results = wifiManager.getScanResults();
        List<ScanResult> filteredResults = filterAndSortResults(results);
        wifiListAdapter.updateData(filteredResults);
        updateVisualization(filteredResults);
    }

    private void scanFailure() {
        List<ScanResult> results = wifiManager.getScanResults();

        if (results.isEmpty()) {
            Toast.makeText(this, "No WiFi networks found", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ScanResult> filteredResults = filterAndSortResults(results);
        wifiListAdapter.updateData(filteredResults);
        updateVisualization(filteredResults);

        Snackbar.make(
                visualizationContainer,
                "Showing cached WiFi scan results",
                Snackbar.LENGTH_LONG
        ).setAction("Retry", v -> startWifiScan()).show();
    }

    private List<ScanResult> filterAndSortResults(List<ScanResult> results) {
        return results.stream()
                .filter(result -> result.SSID != null && !result.SSID.isEmpty())
                .sorted((r1, r2) -> Integer.compare(r2.level, r1.level))
                .collect(Collectors.toList());
    }

    private void updateVisualization(List<ScanResult> wifiPoints) {
        LocationView locationView = new LocationView(this);
        locationView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        for (ScanResult wifiPoint : wifiPoints) {
            locationView.addWifiPoint(new WifiPoint(
                    wifiPoint.SSID,
                    wifiPoint.BSSID,
                    wifiPoint.level,
                    calculateDistance(wifiPoint.level)
            ));
        }

        visualizationContainer.removeAllViews();
        visualizationContainer.addView(locationView);
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

    private void showEnableLocationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Location Required")
                .setMessage("Please enable location services to scan for Wi-Fi networks")
                .setPositiveButton("Enable", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                // Permissions granted, proceed with functionality
                startWifiScan();
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show();
            }
        }
    }
}