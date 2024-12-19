package dev.nimrod.locafi.services;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import dev.nimrod.locafi.models.WifiPoint;
import dev.nimrod.locafi.models.WifiPosition;

public class MapDataService extends Service {
    private static final String TAG = "MapDataService";

    private final IBinder binder = new LocalBinder();
    private WifiManager wifiManager;
    private boolean isPreciseLocation = false;
    private MutableLiveData<List<WifiPoint>> wifiPointsData = new MutableLiveData<>();
    private MutableLiveData<Location> userLocationData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isMapReady = new MutableLiveData<>(false);
    private Handler scanHandler = new Handler(Looper.getMainLooper());
    private static final long SCAN_INTERVAL = 10000; // 10 seconds
    private BroadcastReceiver wifiScanReceiver;

    public class LocalBinder extends Binder {
        public MapDataService getService() {
            return MapDataService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializeService();
        startDataCollection();
    }

    private void initializeService() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        registerWifiScanReceiver();
    }

    private void registerWifiScanReceiver() {
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                if (success) {
                    processScanResults();
                }
            }
        };
        registerReceiver(wifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void startDataCollection() {
        startPeriodicWifiScans();
        isMapReady.setValue(true);
    }

    private void startPeriodicWifiScans() {
        scanHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performWifiScan();
                scanHandler.postDelayed(this, SCAN_INTERVAL);
            }
        }, 0); // Start immediately
    }

    private void performWifiScan() {
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            boolean success = wifiManager.startScan();
            if (!success) {
                processScanResults(); // Use cached results if scan failed
            }
        }
    }

    private void processScanResults() {
        if (wifiManager != null && checkWifiPermissions()) {
            try {
                List<ScanResult> results = wifiManager.getScanResults();
                List<WifiPoint> wifiPoints = convertToWifiPoints(results);
                wifiPointsData.postValue(wifiPoints);
            } catch (SecurityException e) {
                Log.e(TAG, "Security Exception when getting scan results", e);
                // Handle the permission denial gracefully
                wifiPointsData.postValue(new ArrayList<>()); // Empty list
            }
        }
    }
    private boolean checkWifiPermissions() {
        Context context = getApplicationContext();
        boolean hasWifiState = context.checkSelfPermission(Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasWifiChange = context.checkSelfPermission(Manifest.permission.CHANGE_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED;

        // For Android 10 (API 29) and above
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return hasWifiState && hasWifiChange;
        }
        // For older versions
        return hasWifiState;
    }

    private List<WifiPoint> convertToWifiPoints(List<ScanResult> scanResults) {
        List<WifiPoint> wifiPoints = new ArrayList<>();
        for (ScanResult result : scanResults) {
            if (result.SSID != null && !result.SSID.isEmpty()) {
                double distance = calculateDistance(result.level);
                WifiPosition position = calculateWifiPosition(result);

                WifiPoint wifiPoint = new WifiPoint(
                        result.SSID,
                        result.BSSID,
                        result.level,
                        distance,
                        position
                );
                wifiPoints.add(wifiPoint);
            }
        }
        return wifiPoints;
    }

    private double calculateDistance(int rssi) {
        double referenceDistance = 1.0;
        double referenceRSSI = -40.0;
        double pathLossExponent = 2.7;
        return referenceDistance * Math.pow(10.0,
                (referenceRSSI - rssi) / (10.0 * pathLossExponent));
    }

    private WifiPosition calculateWifiPosition(ScanResult result) {
        // For initial position, use a basic calculation
        // This can be enhanced with actual user location later
        double baseLatitude = 0.0; // Will be updated with actual location
        double baseLongitude = 0.0;
        double distance = calculateDistance(result.level);
        double bearing = (result.level + 100) * 3.6; // Convert signal strength to angle

        return new WifiPosition(baseLatitude, baseLongitude)
                .calculateDestination(distance, bearing);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public MutableLiveData<List<WifiPoint>> getWifiPointsData() {
        return wifiPointsData;
    }

    public MutableLiveData<Location> getUserLocationData() {
        return userLocationData;
    }

    public MutableLiveData<Boolean> getIsMapReady() {
        return isMapReady;
    }

    public void startWithPreciseLocation() {
        // Initialize with precise location capabilities
        startDataCollection(true);
    }

    public void startWithApproximateLocation() {
        // Initialize with approximate location
        startDataCollection(false);
    }

    private void startDataCollection(boolean isPrecise) {
        this.isPreciseLocation = isPrecise;
        startPeriodicWifiScans();
        isMapReady.setValue(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wifiScanReceiver != null) {
            unregisterReceiver(wifiScanReceiver);
        }
        scanHandler.removeCallbacksAndMessages(null);
    }
}