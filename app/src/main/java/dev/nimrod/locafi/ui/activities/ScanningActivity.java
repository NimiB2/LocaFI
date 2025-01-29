package dev.nimrod.locafi.ui.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.services.WiFiScanService;
import dev.nimrod.locafi.ui.adapters.WiFiDevicesAdapter;
import dev.nimrod.locafi.utils.FirebaseRepo;

public class ScanningActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private RecyclerView recyclerView;
    private MaterialButton startButton;
    private MaterialButton stopButton;
    private MaterialButton clearButton;
    private View emptyView;
    private FirebaseRepo firebaseRepo;
    private boolean isServiceRunning = false;

    private static final String[] REQUIRED_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE_LOCATION
    };

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WiFiScanService.SCAN_RESULTS_UPDATE.equals(intent.getAction())) {
                loadWiFiDevices();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanning);

        setupViews();
        setupButtons();
        firebaseRepo = new FirebaseRepo();

        // Register for updates from service
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(updateReceiver, new IntentFilter(WiFiScanService.SCAN_RESULTS_UPDATE));

        loadWiFiDevices();
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.scanning_RCV_wifiList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        startButton = findViewById(R.id.scanning_BTN_start);
        stopButton = findViewById(R.id.scanning_BTN_stop);
        clearButton = findViewById(R.id.scanning_BTN_clear);
        emptyView = findViewById(R.id.scanning_LLC_empty_list);

        // Setup toolbar
        findViewById(R.id.scanning_BTN_return_gateway).setOnClickListener(v -> {
            Intent intent = new Intent(ScanningActivity.this, GatewayActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void setupButtons() {
        startButton.setOnClickListener(v -> startScanning());
        stopButton.setOnClickListener(v -> stopScanning());
        clearButton.setOnClickListener(v -> showClearConfirmationDialog());

        findViewById(R.id.scanning_BTN_add_test).setOnClickListener(v -> {
            firebaseRepo.addTestData();
            Toast.makeText(this, "Added test WiFi devices", Toast.LENGTH_SHORT).show();
        });
    }

    private void startScanning() {
        if (!hasLocationPermission()) {
            requestLocationPermission();
            return;
        }

        Intent serviceIntent = new Intent(this, WiFiScanService.class);
        startService(serviceIntent);
        isServiceRunning = true;
        updateButtonStates();
    }

    private void stopScanning() {
        Intent serviceIntent = new Intent(this, WiFiScanService.class);
        serviceIntent.setAction(WiFiScanService.ACTION_STOP_SERVICE);
        startService(serviceIntent);
        isServiceRunning = false;
        updateButtonStates();
    }

    private void updateButtonStates() {
        startButton.setEnabled(!isServiceRunning);
        stopButton.setEnabled(isServiceRunning);
    }

    private void showClearConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("Are you sure you want to clear all saved WiFi devices?")
                .setPositiveButton("Clear", (dialog, which) -> clearAllData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllData() {
        firebaseRepo.clearAllDevices(success -> {
            if (success) {
                Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to clear data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadWiFiDevices() {
        firebaseRepo.getAllDevices(devices -> {
            if (devices != null && !devices.isEmpty()) {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                recyclerView.setAdapter(new WiFiDevicesAdapter(devices));
            } else {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            }
        });
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Location permission required for WiFi scanning",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(updateReceiver);
    }
}