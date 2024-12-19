package dev.nimrod.locafi.ui.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.ui.adapters.WifiListAdapter;

public class WifiManagement extends AppCompatActivity implements WifiListAdapter.OnWifiPointClickListener {

    // UI Components
    private RecyclerView availableWifiRecyclerView;
    private MaterialToolbar toolbar;

    private MaterialButton scanButton;
    private MaterialButton addButton;
    private CircularProgressIndicator loadingIndicator;
    private MaterialTextView selectedNameTextView;
    private MaterialTextView selectedStrengthTextView;
    private MaterialTextView selectedDistanceTextView;

    // Adapters
    private WifiListAdapter wifiListAdapter;

    // Utilities
    private WifiManager wifiManager;
    private ScanResult selectedWifiPoint;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wifi_management);
        initializeServices();
        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupListeners();

    }

    private void initializeServices() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    private void initializeViews() {
        availableWifiRecyclerView = findViewById(R.id.wifi_manage_RCV_available);
        scanButton = findViewById(R.id.wifi_manage_BTN_scan);
        addButton = findViewById(R.id.wifi_manage_BTN_add);
        loadingIndicator = findViewById(R.id.wifi_manage_PGI_loading);
        selectedNameTextView = findViewById(R.id.wifi_manage_MTV_name);
        selectedStrengthTextView = findViewById(R.id.wifi_manage_MTV_strength);
        selectedDistanceTextView = findViewById(R.id.wifi_manage_MTV_distance);
        toolbar = findViewById(R.id.main_MTB_toolbar);

        // Initially hide selected WiFi details and disable add button
        setSelectedWifiVisibility(false);
        addButton.setEnabled(false);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("WiFi Management");
        }
    }

    private void setupRecyclerView() {
        wifiListAdapter = new WifiListAdapter(new ArrayList<>());
        wifiListAdapter.setOnWifiPointClickListener(this);
        availableWifiRecyclerView.setAdapter(wifiListAdapter);
        availableWifiRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        scanButton.setOnClickListener(v -> startWifiScan());
        addButton.setOnClickListener(v -> addSelectedWifiPoint());
    }

    private void startWifiScan() {
        // Add check for WiFi enabled state
        if (!wifiManager.isWifiEnabled()) {
            showEnableWifiDialog();
            return;
        }

        loadingIndicator.setVisibility(View.VISIBLE);
        boolean success = wifiManager.startScan();
        if (!success) {
            loadingIndicator.setVisibility(View.GONE);
            Toast.makeText(this, "Failed to start Wi-Fi scan", Toast.LENGTH_SHORT).show();
        }
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


    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            loadingIndicator.setVisibility(View.GONE);
            if (success) {
                scanSuccess();
            } else {
                scanFailure();
            }
        }
    };

    private void scanSuccess() {
        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        wifiListAdapter.updateData(results);
    }

    private void scanFailure() {
        @SuppressLint("MissingPermission") List<ScanResult> results = wifiManager.getScanResults();
        if (results.isEmpty()) {
            Toast.makeText(this, "No WiFi networks found", Toast.LENGTH_SHORT).show();
            return;
        }
        wifiListAdapter.updateData(results);
        Toast.makeText(this, "Showing cached WiFi scan results", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onWifiPointClick(ScanResult wifiPoint) {
        selectedWifiPoint = wifiPoint;
        updateSelectedWifiDetails();
        setSelectedWifiVisibility(true);
        addButton.setEnabled(true);
    }

    private void updateSelectedWifiDetails() {
        if (selectedWifiPoint != null) {
            selectedNameTextView.setText(selectedWifiPoint.SSID);
            selectedStrengthTextView.setText(String.format("Signal Strength: %d dBm", selectedWifiPoint.level));
            WifiPoint tempPoint = new WifiPoint(
                    selectedWifiPoint.SSID,
                    selectedWifiPoint.BSSID,
                    selectedWifiPoint.level
            );
            selectedDistanceTextView.setText(String.format("Approximate Distance: %.2f meters", tempPoint.getDistance()));
        }
    }

    private double calculateDistance(int rssi) {
        double referenceDistance = 1.0;
        double referenceRSSI = -40.0;
        double pathLossExponent = 2.7;
        return referenceDistance * Math.pow(10.0, (referenceRSSI - rssi) / (10.0 * pathLossExponent));
    }

    private void setSelectedWifiVisibility(boolean visible) {
        int visibility = visible ? View.VISIBLE : View.GONE;
        selectedNameTextView.setVisibility(visibility);
        selectedStrengthTextView.setVisibility(visibility);
        selectedDistanceTextView.setVisibility(visibility);
        addButton.setVisibility(visibility);
    }

    private void addSelectedWifiPoint() {
        // TODO: Implement storage of selected WiFi point
        Toast.makeText(this, "Added " + selectedWifiPoint.SSID, Toast.LENGTH_SHORT).show();
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
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}