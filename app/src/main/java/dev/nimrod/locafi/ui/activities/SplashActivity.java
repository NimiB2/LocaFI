package dev.nimrod.locafi.ui.activities;


import android.Manifest;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        logoImage = findViewById(R.id.splash_IMG_logo);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Start initialization
        initializeServices();
    }

    private void initializeServices() {
        // Only check if services are enabled
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isLocationEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isWifiEnabled = wifiManager.isWifiEnabled();

        if (!isLocationEnabled || !isWifiEnabled) {
            showServiceEnableDialog();
            return;
        }

        // Start animation directly
        startAnimation();
    }


    private void startServiceAndAnimation(Location location) {
        // Start the service with initial location
        Intent serviceIntent = new Intent(this, MapDataService.class);
        if (location != null) {
            serviceIntent.putExtra("initial_latitude", location.getLatitude());
            serviceIntent.putExtra("initial_longitude", location.getLongitude());
        }
        startService(serviceIntent);

        // Start animation
        startAnimation();
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
                .show();
    }

    private void startAnimation() {
        logoImage.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .alpha(1f)
                .setDuration(1000)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        startMainActivity();
                    }

                    @Override public void onAnimationStart(Animator animation) {}
                    @Override public void onAnimationCancel(Animator animation) {}
                    @Override public void onAnimationRepeat(Animator animation) {}
                });
    }


    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}