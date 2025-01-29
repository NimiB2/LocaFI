package dev.nimrod.locafi.ui.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import android.Manifest;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.services.MapDataService;
import dev.nimrod.locafi.ui.adapters.WiFiDevicesAdapter;
import dev.nimrod.locafi.ui.adapters.WifiListAdapter;
import dev.nimrod.locafi.ui.views.MapFragment;
import dev.nimrod.locafi.utils.FirebaseRepo;
import dev.nimrod.locafi.utils.LocationPermissionHandler;


public class MainActivity extends AppCompatActivity {
    private View mainLayout;

    // Views from activity_main.xml
    private MaterialCardView mainMCVVisualization;
    private CircularProgressIndicator mainPGILoading;
    private View mainVISLocation;
    private MaterialCardView mainMCVWifiList;
    private View mainLLCEmptyList;
    private RecyclerView mainRCVWifiList;
    private View mainLLCButtons;



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
        // "Visualization Container" card
        mainMCVVisualization = findViewById(R.id.main_MCV_visualization);

        // Progress indicator inside the visualization card
        mainPGILoading = findViewById(R.id.main_PGI_loading);

        // FrameLayout for location visualization (currently hidden)
        mainVISLocation = findViewById(R.id.main_VIS_location);

        // Wi-Fi List Card
        mainMCVWifiList = findViewById(R.id.main_MCV_wifiList);

        // "Empty list" layout
        mainLLCEmptyList = findViewById(R.id.main_LLC_empty_list);

        // RecyclerView for showing Wi-Fi devices
        mainRCVWifiList = findViewById(R.id.main_RCV_wifiList);
        mainRCVWifiList.setLayoutManager(new LinearLayoutManager(this));

        // Bottom Buttons container
        mainLLCButtons = findViewById(R.id.main_LLC_buttons);
    }


    private void initButtons() {
        // "Your Exact GPS Location" button
        findViewById(R.id.main_BTN_location).setOnClickListener(view -> {
            // TODO: Show your map or get the user’s exact location
            Toast.makeText(MainActivity.this,
                    "Show user's exact location (TODO)",
                    Toast.LENGTH_SHORT).show();
        });

        MaterialToolbar toolbar = findViewById(R.id.main_ABL_appbar);
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

        // Example using a hypothetical FirebaseRepo
        FirebaseRepo repo = new FirebaseRepo();
        repo.getAllDevices(devices -> {
            showLoading(false);
            if (devices == null || devices.isEmpty()) {
                showEmptyList(true);
            } else {
                showEmptyList(false);
                updateRecyclerView(devices);
            }
        });

    }


    private void showLoading(boolean isLoading) {
        if (isLoading) {
            mainPGILoading.setVisibility(View.VISIBLE);
        } else {
            mainPGILoading.setVisibility(View.GONE);
        }
    }


    private void showEmptyList(boolean showEmpty) {
        if (showEmpty) {
            mainLLCEmptyList.setVisibility(View.VISIBLE);
            mainRCVWifiList.setVisibility(View.GONE);
        } else {
            mainLLCEmptyList.setVisibility(View.GONE);
            mainRCVWifiList.setVisibility(View.VISIBLE);
        }
    }

    private void updateRecyclerView(List<WiFiDevice> devices) {
        WiFiDevicesAdapter adapter = new WiFiDevicesAdapter(devices);
        mainRCVWifiList.setAdapter(adapter);


        // For a quick placeholder, let's just log or do something minimal
        Toast.makeText(this,
                "Loaded " + devices.size() + " Wi-Fi devices",
                Toast.LENGTH_SHORT).show();
    }
}