package dev.nimrod.locafi.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.ui.adapters.WiFiDevicesAdapter;
import dev.nimrod.locafi.utils.FirebaseRepo;
import dev.nimrod.locafi.maps.WifiMapFragment;
import dev.nimrod.locafi.utils.LocationCalculator;


public class MainActivity extends AppCompatActivity implements WiFiDevicesAdapter.OnWiFiDeviceClickListener {    private View mainLayout;
    private MaterialCardView mainMCVVisualization;
    private CircularProgressIndicator mainPGILoading;
    private View mainVISLocation;
    private MaterialCardView mainMCVWifiList;
    private View mainLLCEmptyList;
    private RecyclerView mainRCVWifiList;
    private View mainLLCButtons;

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

        mainLayout = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        initViews();
        initButtons();

        loadWiFiDevices();
    }


    private void initViews() {
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

        firebaseRepo = new FirebaseRepo();
    }

    private void initButtons() {
        // "Your Exact GPS Location" button
        findViewById(R.id.main_BTN_location).setOnClickListener(view -> {
            showEstimatedLocation();
        });

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

    @Override
    public void onWiFiDeviceClick(WiFiDevice device) {
        if (wifiMapFragment != null) {
            wifiMapFragment.zoomToDevice(device);
        }
    }
}