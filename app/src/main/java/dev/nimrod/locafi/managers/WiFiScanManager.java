package dev.nimrod.locafi.managers;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import dev.nimrod.locafi.models.WiFiDevice;

public class WiFiScanManager {

    private final Context context;
    private final WifiManager wifiManager;
    private final FusedLocationProviderClient fusedLocationClient;

    private ScanCallback callback;
    private BroadcastReceiver wifiScanReceiver;

    public WiFiScanManager(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

    }

    private boolean hasRequiredPermissions(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                    && context.checkSelfPermission(android.Manifest.permission.ACCESS_WIFI_STATE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public void startScan(ScanCallback callback) {
        this.callback = callback;

        if (!hasRequiredPermissions(context)) {
            Log.e("WiFiScanManager", "Cannot start scan - missing permissions");
            if (callback != null) {
                callback.onScanResults(new ArrayList<>()); // Return empty list if no permissions
            }
            return;
        }

        if (!isWifiEnabled()) {
            Log.e("WiFiScanManager", "Cannot start scan - WiFi is disabled");
            if (callback != null) {
                callback.onScanResults(new ArrayList<>()); // Return empty list if WiFi is disabled
            }
            return;
        }

        registerWifiScanReceiver();
        beginScanCycle();
    }
    private void beginScanCycle() {
        if (wifiManager != null) {
            wifiManager.startScan();
        }
        // The BroadcastReceiver will handle the results
    }

    public void stopScan() {
        unregisterWifiScanReceiver();
    }
    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    private void registerWifiScanReceiver() {
        if (wifiScanReceiver == null) {
            wifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, android.content.Intent intent) {
                    getCurrentLocationAndScanWifi();
                }
            };
            context.registerReceiver(wifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
    }

    private void unregisterWifiScanReceiver() {
        if (wifiScanReceiver != null) {
            context.unregisterReceiver(wifiScanReceiver);
            wifiScanReceiver = null;
        }
    }

    private void getCurrentLocationAndScanWifi() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        processScanResults(location);
                    }
                });
    }

    private void processScanResults(Location location) {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<WiFiDevice> devices = new ArrayList<>();

        if (scanResults != null) {
            for (ScanResult sr : scanResults) {
                WiFiDevice device = new WiFiDevice();
                device.setSsid(sr.SSID);
                device.setBssid(sr.BSSID);
                device.setSignalStrength(sr.level);
                device.setLatitude(location.getLatitude());
                device.setLongitude(location.getLongitude());
                device.setTimestamp(System.currentTimeMillis());
                devices.add(device);
            }
        }

        if (callback != null) {
            callback.onScanResults(devices);
        }
    }
    // Callback interface for scan results
    public interface ScanCallback {
        void onScanResults(List<WiFiDevice> scannedDevices);
    }
}
