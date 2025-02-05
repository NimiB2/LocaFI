package dev.nimrod.locafi.ui.activities;

import static androidx.core.location.LocationManagerCompat.requestLocationUpdates;
import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.LOCATION_DISABLE;
import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.LOCATION_SETTINGS_OK;
import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.LOCATION_SETTINGS_PROCESS;
import static dev.nimrod.locafi.managers.PermissionManager.PermissionState.NO_REGULAR_PERMISSION;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

import dev.nimrod.locafi.LocaFiApp;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.managers.PermissionManager;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.ui.adapters.WiFiDevicesAdapter;
import dev.nimrod.locafi.utils.FirebaseRepo;
import dev.nimrod.locafi.ui.maps.WifiMapFragment;
import dev.nimrod.locafi.utils.LocationCalculator;


public class MainActivity extends AppCompatActivity implements WiFiDevicesAdapter.OnWiFiDeviceClickListener {
    private View mainLayout;
    private MaterialCardView mainMCVVisualization;
    private CircularProgressIndicator mainPGILoading;
    private View mainVISLocation;
    private MaterialCardView mainMCVWifiList;
    private View mainLLCEmptyList;
    private RecyclerView mainRCVWifiList;
    private View mainLLCButtons;
    private PermissionManager permissionManager;
    private boolean isCheckingPermissions = false;

    private WifiMapFragment wifiMapFragment;
    private FirebaseRepo firebaseRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        initViews();
        initButtons();
        loadWiFiDevices();
    }


    private void initViews() {
        permissionManager = new PermissionManager(this);
        mainMCVVisualization = findViewById(R.id.main_MCV_visualization);
        mainPGILoading = findViewById(R.id.main_PGI_loading);
        mainVISLocation = findViewById(R.id.main_VIS_location);
        mainMCVWifiList = findViewById(R.id.main_MCV_wifiList);
        mainLLCEmptyList = findViewById(R.id.main_LLC_empty_list);
        mainRCVWifiList = findViewById(R.id.main_RCV_wifiList);
        mainRCVWifiList.setLayoutManager(new LinearLayoutManager(this));
        mainLLCButtons = findViewById(R.id.main_LLC_buttons);

        if (mainVISLocation != null) {
            mainVISLocation.setVisibility(View.VISIBLE);
            wifiMapFragment = new WifiMapFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_VIS_location, wifiMapFragment)
                    .commitNow();
        } else {
            Log.e("MainActivity", "Error: mainVISLocation view not found!");
        }

        firebaseRepo = new FirebaseRepo(LocaFiApp.getCurrentUser().getUserId());
    }

    private void initButtons() {
        // "Your Exact GPS Location" button
        findViewById(R.id.main_BTN_location).setOnClickListener(view -> {
            showEstimatedLocation();
        });
        setupGPSLocationButton();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GatewayActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void loadWiFiDevices() {
        showLoading(true);
        new Thread(() -> {
            firebaseRepo.getAllDevices(devices -> {
                runOnUiThread(() -> {
                    showLoading(false);
                    if (devices == null || devices.isEmpty()) {
                        showEmptyList(true);
                    } else {
                        showEmptyList(false);
                        updateRecyclerView(devices);
                        if (mainVISLocation != null) {
                            mainVISLocation.setVisibility(View.VISIBLE);
                        }
                        if (wifiMapFragment != null) {
                            wifiMapFragment.updateWiFiDevices(devices);
                        }
                    }
                });
            });
        }).start();
    }

    private void showEstimatedLocation() {
        firebaseRepo.getAllDevices(devices -> {
            if (devices == null || devices.isEmpty()) {
                Toast.makeText(MainActivity.this,
                        "No WiFi devices available for location estimation",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            LatLng estimatedLocation = LocationCalculator.calculateLocation(devices);
            if (estimatedLocation != null && wifiMapFragment != null) {
                wifiMapFragment.zoomToLocation(estimatedLocation);
                updateLocationTexts(estimatedLocation, false);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (mainPGILoading != null) {
            mainPGILoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
    }

    private void showEmptyList(boolean showEmpty) {
        if (mainLLCEmptyList != null && mainRCVWifiList != null) {
            mainLLCEmptyList.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
            mainRCVWifiList.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private void updateRecyclerView(List<WiFiDevice> devices) {
        if (mainRCVWifiList != null) {
            WiFiDevicesAdapter adapter = new WiFiDevicesAdapter(devices);
            adapter.setOnWiFiDeviceClickListener(this);
            mainRCVWifiList.setAdapter(adapter);
        }
    }


    private void setupGPSLocationButton() {
        wifiMapFragment.setIsScanning(false);
        MaterialButton gpsButton = findViewById(R.id.main_BTN_gps_location);
        gpsButton.setOnClickListener(v -> {
            if (!isLocationEnabled()) {
                showEnableLocationServicesDialog();
                return;
            }
            requestLocationPermissionAndGetLocation();
        });
    }

    private void showEnableLocationServicesDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Enable Location")
                .setMessage("Your location services are disabled. Please enable them to use GPS location.")
                .setPositiveButton("Location Settings", (dialogInterface, i) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestLocationPermissionAndGetLocation() {
        if (permissionManager.hasLocationPermission()) {
            getAndShowLocation();
            return;
        }

        if (!isCheckingPermissions) {
            isCheckingPermissions = true;
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
                            getAndShowLocation();
                            break;
                    }
                }

                @Override
                public void onLocationSettingsResult(boolean isEnabled) {
                    if (isEnabled) {
                        getAndShowLocation();
                    } else {
                        permissionManager.openLocationSettings();
                    }
                }
            });
        }
    }

    private void updateLocationTexts(LatLng location, boolean isGPS) {
        String locationText = String.format("%.6f, %.6f",
                location.latitude, location.longitude);

        if (isGPS) {
            ((TextView) findViewById(R.id.main_TXT_gps_location)).setText(locationText);
        } else {
            ((TextView) findViewById(R.id.main_TXT_estimated_location)).setText(locationText);
        }

        // Update error distance if both locations are available
        if (wifiMapFragment != null) {
            float distance = wifiMapFragment.calculateDistance();
            if (distance >= 0) {
                String errorText = String.format("Distance between locations: %.2f meters", distance);
                ((TextView) findViewById(R.id.main_TXT_error_distance)).setText(errorText);
            }
        }
    }



    private boolean isLocationEnabled() {
        android.location.LocationManager locationManager =
                (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null &&
                (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                        locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER));
    }

    private void getAndShowLocation() {
        try {
            FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(this);
            locationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    if (wifiMapFragment != null) {
                        wifiMapFragment.toggleGPSMarker(userLocation);
                        updateLocationTexts(userLocation, true);
                        MaterialButton gpsButton = findViewById(R.id.main_BTN_gps_location);
                        if (wifiMapFragment.isGpsLocationVisible()) {
                            gpsButton.setText("Hide GPS Location");
                            wifiMapFragment.zoomToGPSLocation(userLocation);
                        } else {
                            gpsButton.setText("Show GPS Location");
                        }
                    }
                } else {
                    Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (SecurityException e) {
            Log.e("MainActivity", "Error getting location: " + e.getMessage());
        }
    }

    @Override
    public void onWiFiDeviceClick(WiFiDevice device) {
        if (wifiMapFragment != null) {
            wifiMapFragment.zoomToDevice(device);
        }
    }
}