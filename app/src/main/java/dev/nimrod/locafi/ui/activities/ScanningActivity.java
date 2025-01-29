package dev.nimrod.locafi.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.managers.WiFiScanManager;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.ui.adapters.WiFiDevicesAdapter;

public class ScanningActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private WiFiScanManager wifiScanManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge (your existing code)
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanning);

        View mainLayout = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize WiFiScanManager (ADDED)
        wifiScanManager = new WiFiScanManager(this);

        checkWifiEnabled();
        // Set up button click listeners
        initViews();
    }

    private void initViews() {
        // "Start Wi-Fi Scan" Button
        findViewById(R.id.scanning_BTN_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasLocationPermission()) {
                    startWiFiScan();
                    Toast.makeText(ScanningActivity.this, "Starting WiFi scan...", Toast.LENGTH_SHORT).show();
                } else {
                    requestLocationPermission();
                }
            }
        });

        // "Stop Wi-Fi Scan" Button
        findViewById(R.id.scanning_BTN_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWiFiScan();
            }
        });

        // "Return to Gateway" Button
        findViewById(R.id.scanning_BTN_return_gateway).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ScanningActivity.this, GatewayActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    private void checkWifiEnabled() {
        if (wifiScanManager != null && !wifiScanManager.isWifiEnabled()) {
            Toast.makeText(this, "Please enable WiFi to scan networks", Toast.LENGTH_LONG).show();
            startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
        }
    }

    private void startWiFiScan() {
        wifiScanManager.startScan(new WiFiScanManager.ScanCallback() {
            @Override
            public void onScanResults(List<WiFiDevice> scannedDevices) {
                runOnUiThread(() -> {
                    if (scannedDevices.isEmpty()) {
                        findViewById(R.id.scanning_LLC_empty_list).setVisibility(View.VISIBLE);
                        findViewById(R.id.scanning_RCV_wifiList).setVisibility(View.GONE);
                    } else {
                        findViewById(R.id.scanning_LLC_empty_list).setVisibility(View.GONE);
                        findViewById(R.id.scanning_RCV_wifiList).setVisibility(View.VISIBLE);

                        RecyclerView recyclerView = findViewById(R.id.scanning_RCV_wifiList);
                        recyclerView.setLayoutManager(new LinearLayoutManager(ScanningActivity.this));
                        recyclerView.setAdapter(new WiFiDevicesAdapter(scannedDevices));
                    }
                });

                Toast.makeText(ScanningActivity.this,
                        "Scan complete! Found " + scannedDevices.size() + " networks",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void stopWiFiScan() {
        wifiScanManager.stopScan();
        Toast.makeText(this, "Stopping Wi-Fi Scan...", Toast.LENGTH_SHORT).show();
    }


    private boolean hasLocationPermission() {
        // For Android 6.0+, location permission is required to get scan results
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED;
        }
        // On older versions, permission is granted at install time
        return true;
    }


    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                startWiFiScan();
            } else {
                // Permission denied
                Toast.makeText(this,
                        "Location permission denied. Cannot scan Wi-Fi networks.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}