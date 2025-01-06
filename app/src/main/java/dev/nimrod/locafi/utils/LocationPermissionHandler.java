package dev.nimrod.locafi.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class LocationPermissionHandler {
    private static final String STATE_PREF = "PermissionStatePref";
    private static final String CURRENT_STATE_KEY = "CurrentPermissionState";
    private final AppCompatActivity activity;
    private final PermissionCallback callback;
    private PermissionState currentState;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;

    public enum PermissionState {
        INITIAL_REQUEST,
        DETAILED_REQUEST,
        MANUAL_SETTINGS
    }

    public interface PermissionCallback {
        void onPermissionGranted();
    }

    public LocationPermissionHandler(AppCompatActivity activity, PermissionCallback callback) {
        this.activity = activity;
        this.callback = callback;
        this.currentState = PermissionState.valueOf(
                activity.getSharedPreferences(STATE_PREF, Context.MODE_PRIVATE)
                        .getString(CURRENT_STATE_KEY, PermissionState.INITIAL_REQUEST.name())
        );
        initializePermissionLaunchers();
    }

    private void initializePermissionLaunchers() {
        requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                this::handlePermissionResult
        );

        settingsLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> checkPermissionAfterSettings()
        );
    }

    public void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            callback.onPermissionGranted();
            return;
        }

        switch (currentState) {
            case INITIAL_REQUEST:
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                break;
            case DETAILED_REQUEST:
                showDetailedPermissionDialog();
                break;
            case MANUAL_SETTINGS:
                showSettingsDialog();
                break;
        }
    }

    private void showDetailedPermissionDialog() {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Access Your Location")
                .setMessage("We need your location to find and display nearby WiFi networks on the map. Without this permission, the app can't discover networks in your area.")
                .setPositiveButton("Continue", (dialog, which) ->
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION))
                .setNegativeButton("Cancel", (dialog, which) -> activity.finishAffinity())
                .setCancelable(false)
                .show();
    }

    private void showSettingsDialog() {
        new MaterialAlertDialogBuilder(activity)
                .setTitle("Location Permission Required")
                .setMessage("Please enable location permission in Settings to use this feature.")
                .setPositiveButton("Open Settings", (dialog, which) -> openSettings())
                .setNegativeButton("Cancel", (dialog, which) -> activity.finishAffinity())
                .show();
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        settingsLauncher.launch(intent);
    }

    private void checkPermissionAfterSettings() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            callback.onPermissionGranted();
        } else {
            activity.finishAffinity();
        }
    }

    private void handlePermissionResult(boolean isGranted) {
        if (isGranted) {
            callback.onPermissionGranted();
            return;
        }

        switch (currentState) {
            case INITIAL_REQUEST:
                currentState = PermissionState.DETAILED_REQUEST;
                saveCurrentState();
                showDetailedPermissionDialog();
                break;
            case DETAILED_REQUEST:
                currentState = PermissionState.MANUAL_SETTINGS;
                saveCurrentState();
                showSettingsDialog();
                break;
            case MANUAL_SETTINGS:
                activity.finishAffinity();
                break;
        }
    }

    private void saveCurrentState() {
        activity.getSharedPreferences(STATE_PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(CURRENT_STATE_KEY, currentState.name())
                .apply();
    }
}
