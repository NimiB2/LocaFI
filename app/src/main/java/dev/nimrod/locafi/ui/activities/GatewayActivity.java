package dev.nimrod.locafi.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import dev.nimrod.locafi.R;

public class GatewayActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isCheckingPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway);

        findViewById(R.id.gateway_BTN_scan).setOnClickListener(v -> {
            if (!isCheckingPermissions) {
                isCheckingPermissions = true;
                executor.execute(() -> checkAndRequestPermissions());
            }
        });

        findViewById(R.id.gateway_BTN_main).setOnClickListener(v -> {
            executor.execute(() -> {
                mainHandler.post(() -> {
                    startActivity(new Intent(GatewayActivity.this, MainActivity.class));
                });
            });
        });
    }

    private void checkAndRequestPermissions() {
        mainHandler.post(() -> {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startScanningActivity();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showPermissionExplanationDialog();
                } else {
                    requestLocationPermission();
                }
            }
            isCheckingPermissions = false;
        });
    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("WiFi scanning requires location access. Please grant the permission to proceed.")
                .setPositiveButton("Grant Permission", (dialog, which) -> requestLocationPermission())
                .setNegativeButton("Cancel", (dialog, which) -> showSettingsMessage())
                .show();
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startScanningActivity() {
        startActivity(new Intent(this, ScanningActivity.class));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                executor.execute(() -> mainHandler.post(this::startScanningActivity));
            } else {
                showSettingsMessage();
            }
        }
    }

    private void showSettingsMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("You must enable location permissions in settings to access the scan feature.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) ->
                        Toast.makeText(this, "Cannot proceed without permissions.",
                                Toast.LENGTH_LONG).show())
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}