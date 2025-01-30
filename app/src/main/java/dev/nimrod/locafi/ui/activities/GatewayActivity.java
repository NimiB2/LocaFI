package dev.nimrod.locafi.ui.activities;

import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.LOCATION_DISABLE;
import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.LOCATION_SETTINGS_OK;
import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.LOCATION_SETTINGS_PROCESS;
import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.NO_REGULAR_PERMISSION;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.managers.PermissionManager;

public class GatewayActivity extends AppCompatActivity {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private PermissionManager permissionManager;
    private boolean isCheckingPermissions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gateway);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gateway_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        initButtons();

    }

    private void initButtons() {
        findViewById(R.id.gateway_BTN_scan).setOnClickListener(v -> {
            if (!isCheckingPermissions) {
                isCheckingPermissions = true;
                checkPermissionsAndStartScanning();
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

    private void initViews() {
        permissionManager = new PermissionManager(this);
    }

    private void checkPermissionsAndStartScanning() {
        if (permissionManager.hasLocationPermission()) {
            isCheckingPermissions = false;
            startActivity(new Intent(GatewayActivity.this, ScanningActivity.class));
            return;
        }

        permissionManager.requestLocationPermission(new PermissionManager.PermissionCallback() {
            @Override
            public void onPermissionResult(PermissionManager.PermissionState state) {
                isCheckingPermissions = false;
                switch (state) {
                    case LOCATION_DISABLE:
                        permissionManager.openLocationSettings();
                        break;
                    case NO_REGULAR_PERMISSION:
                        permissionManager.requestRegularPermissions();
                        break;
                    case LOCATION_SETTINGS_PROCESS:
                        permissionManager.validateLocationSettings();
                        break;
                    case LOCATION_SETTINGS_OK:
                        startActivity(new Intent(GatewayActivity.this, ScanningActivity.class));
                        break;
                }
            }

            @Override
            public void onLocationSettingsResult(boolean isEnabled) {
                if (isEnabled) {
                    startActivity(new Intent(GatewayActivity.this, ScanningActivity.class));
                } else {
                    permissionManager.openLocationSettings();
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}