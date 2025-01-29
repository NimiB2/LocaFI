package dev.nimrod.locafi.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;

import dev.nimrod.locafi.models.WiFiDevice;

public class WiFiScanManager {

    private final Context context;
    private final WifiManager wifiManager;
    private ScanCallback callback;
    private BroadcastReceiver wifiScanReceiver;

    public WiFiScanManager(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
    }

    public void startScan(ScanCallback callback) {
        this.callback = callback;
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

    private void registerWifiScanReceiver() {
        if (wifiScanReceiver == null) {
            wifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, android.content.Intent intent) {
                    List<ScanResult> scanResults = wifiManager.getScanResults();
                    List<WiFiDevice> devices = new ArrayList<>();
                    if (scanResults != null) {
                        for (ScanResult sr : scanResults) {
                            WiFiDevice device = new WiFiDevice();
                            device.setSsid(sr.SSID);
                            device.setBssid(sr.BSSID);
                            device.setSignalStrength(sr.level);
                            // location can be set if you have any location logic
                            devices.add(device);
                        }
                    }
                    if (callback != null) {
                        callback.onScanResults(devices);
                    }
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

    // Callback interface for scan results
    public interface ScanCallback {
        void onScanResults(List<WiFiDevice> scannedDevices);
    }
}
