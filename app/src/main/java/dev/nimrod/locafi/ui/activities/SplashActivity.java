package dev.nimrod.locafi.ui.activities;


import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.services.MapDataService;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";
    private ShapeableImageView logoImage;
    private MapDataService mapService;
    private ServiceConnection serviceConnection;
    private boolean isServiceReady = false;
    private boolean isAnimationComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logoImage = findViewById(R.id.splash_IMG_logo);

        // Start both processes in parallel
        initializeServices();
        startLogoAnimation();
    }

    private void initializeServices() {
        if (!checkServicesEnabled()) {
            showServiceEnableDialog();
            return;
        }

        Intent serviceIntent = new Intent(this, MapDataService.class);
        startService(serviceIntent);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                MapDataService.LocalBinder binder = (MapDataService.LocalBinder) service;
                mapService = binder.getService();
                mapService.initializeIfNeeded();
                mapService.startWithApproximateLocation();
                mapService.preInitializeMap();
                isServiceReady = true;
                tryStartMainActivity();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mapService = null;
            }
        };

        bindService(new Intent(this, MapDataService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private boolean checkServicesEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isWifiEnabled = wifiManager.isWifiEnabled();

        return isLocationEnabled && isWifiEnabled;
    }

    private void showServiceEnableDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Enable Services")
                .setMessage("Location and WiFi services are required for this app to work properly. Would you like to enable them?")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void startLogoAnimation() {
        logoImage.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .alpha(1f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimationComplete = true;
                        tryStartMainActivity();
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {}

                    @Override
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationRepeat(Animator animation) {}
                });
    }

    private void tryStartMainActivity() {
        // Only proceed when both conditions are met
        if (isServiceReady && isAnimationComplete) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceConnection != null && mapService != null) {
            unbindService(serviceConnection);
            serviceConnection = null;
            mapService = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check services again when returning from settings
        if (!checkServicesEnabled()) {
            showServiceEnableDialog();
        } else if (!isServiceReady) {
            // Restart service initialization if needed
            initializeServices();
        }
    }
}