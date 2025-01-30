package dev.nimrod.locafi.managers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.Priority;

public class PermissionManager {
    private static final int PERMISSION_REQUEST_CODE = 1001;
    private boolean settingsDialogShown = false;
    private boolean dialogSuppressed = false;
    private final Context context;
    private PermissionCallback callback;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    public enum PermissionState {
        NA,
        NO_REGULAR_PERMISSION,
        NO_BACKGROUND_PERMISSION,
        LOCATION_DISABLE,
        LOCATION_SETTINGS_PROCESS,
        LOCATION_SETTINGS_OK,
        LOCATION_ENABLE
    }

    public interface PermissionCallback {
        void onPermissionResult(PermissionState state);

        void onLocationSettingsResult(boolean isEnabled);
    }

    public PermissionManager(Context context) {
        this.context = context;
    }

    public void requestLocationPermission(PermissionCallback callback) {
        this.callback = callback;
        dialogSuppressed = false;

        PermissionState currentState = getCurrentState();

        if (callback != null) {
            callback.onPermissionResult(currentState);
        }
    }

    public PermissionState getCurrentState() {
        if (!isLocationEnabled()) {
            return PermissionState.LOCATION_DISABLE;
        }

        if (!hasLocationPermission()) {
            return PermissionState.NO_REGULAR_PERMISSION;
        }

        if (!hasBackgroundLocationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return PermissionState.NO_BACKGROUND_PERMISSION;
        }

        return PermissionState.LOCATION_SETTINGS_PROCESS;
    }


    public boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF);
            return (mode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }

    public void validateLocationSettings() {
        LocationRequest locationRequest = new LocationRequest.Builder(1000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(1.0f)
                .setMinUpdateIntervalMillis(1000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        LocationSettingsRequest settingsRequest = builder.build();
        SettingsClient settingsClient = LocationServices.getSettingsClient(context);

        settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener(locationSettingsResponse -> {
                    PreferencesManager.setPermissionState(context, PermissionState.LOCATION_SETTINGS_OK.name());
                    if (callback != null) {
                        callback.onLocationSettingsResult(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onLocationSettingsResult(false);
                    }
                });
    }

    private boolean hasLocationPermission() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean hasBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public void requestRegularPermissions() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show explanation dialog after first denial
                new AlertDialog.Builder(context)
                        .setTitle("Location Permission Required")
                        .setMessage("Location permission is needed for core functionality.\n" +
                                "Please Enable the app permission to access your location data")
                        .setPositiveButton("I Understand", (dialog, which) -> {
                            ActivityCompat.requestPermissions(
                                    activity,
                                    REQUIRED_PERMISSIONS,
                                    PERMISSION_REQUEST_CODE
                            );
                        })
                        .setCancelable(false)
                        .show();
            } else {
                // First time request or after "Never ask again"
                ActivityCompat.requestPermissions(
                        activity,
                        REQUIRED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }


    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public void requestBackgroundPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (context instanceof Activity) {
                if (shouldShowBackgroundPermissionRationale()) {
                    showBackgroundPermissionRationale();
                } else {
                    ActivityCompat.requestPermissions(
                            (Activity) context,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            PERMISSION_REQUEST_CODE
                    );
                }
            }
        }
    }

    private boolean shouldShowRequestPermissionRationale() {
        if (context instanceof Activity) {
            for (String permission : REQUIRED_PERMISSIONS) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, permission)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldShowBackgroundPermissionRationale() {
        if (context instanceof Activity && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.shouldShowRequestPermissionRationale(
                    (Activity) context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            );
        }
        return false;
    }


    private void showBackgroundPermissionRationale() {
        new AlertDialog.Builder(context)
                .setTitle("Background Location Permission")
                .setMessage("This app collects location data even when the app is closed or not in use.\n" +
                        "To protect your privacy, the app stores only calculated indicators, and never exact location.\n" +
                        "A notification is always displayed when the service is running.")
                .setPositiveButton("Allow", (dialog, which) -> requestBackgroundPermission())
                .setNegativeButton("Cancel", (dialog, which) -> {
                    if (callback != null) {
                        callback.onPermissionResult(PermissionState.NO_BACKGROUND_PERMISSION);
                    }
                })
                .show();
    }

    public void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        if (context instanceof Activity) {
            ((Activity) context).startActivity(intent);
        }
    }


    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (callback != null) {
                    callback.onPermissionResult(PermissionState.LOCATION_SETTINGS_PROCESS);
                }
            } else {
                // Check if "Never ask again" was selected
                if (!ActivityCompat.shouldShowRequestPermissionRationale((Activity) context,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showSettingsDialog();
                } else {
                    if (callback != null) {
                        callback.onPermissionResult(PermissionState.NO_REGULAR_PERMISSION);
                    }
                }
            }
        }
    }

    private void showSettingsDialog() {
        if (settingsDialogShown || dialogSuppressed) {
            return;
        }
        settingsDialogShown = true;

        new AlertDialog.Builder(context)
                .setTitle("Permission Required")
                .setMessage("Location permission is required for some features. " +
                        "You can continue using the app with limited functionality or enable permissions in settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    openAppSettings();
                    settingsDialogShown = false;
                })
                .setNegativeButton("Continue without permission", (dialog, which) -> {
                    dialog.dismiss();
                    dialogSuppressed = true;
                    settingsDialogShown = false;
                    PreferencesManager.setPermissionState(
                            context,
                            PermissionState.NO_REGULAR_PERMISSION.name()
                    );
                    if (callback != null) {
                        callback.onPermissionResult(PermissionState.NO_REGULAR_PERMISSION);
                    }
                })
                .show();
    }
}