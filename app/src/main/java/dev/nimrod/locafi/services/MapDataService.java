package dev.nimrod.locafi.services;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
import dev.nimrod.locafi.models.WifiPoint;

public class MapDataService extends Service {
    private final IBinder binder = new LocalBinder();
    private WifiManager wifiManager;
    private MutableLiveData<List<WifiPoint>> wifiPointsData = new MutableLiveData<>();
    private MutableLiveData<Location> userLocationData = new MutableLiveData<>();
    private MutableLiveData<Boolean> isMapReady = new MutableLiveData<>(false);

    public class LocalBinder extends Binder {
        public MapDataService getService() {
            return MapDataService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        startDataCollection();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startDataCollection() {
        // Start periodic WiFi scans
        // Initialize location updates
        // Set up map loading status
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
}