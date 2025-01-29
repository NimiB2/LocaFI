package dev.nimrod.locafi.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.managers.WiFiScanManager;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.utils.FirebaseRepo;

public class WiFiScanService extends Service {
    private static final String CHANNEL_ID = "WIFI_SCAN_CHANNEL";
    private WiFiScanManager wifiScanManager;
    private boolean isScanning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        wifiScanManager = new WiFiScanManager(this);
        createNotificationChannel();
        startForegroundServiceNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isScanning) {
            isScanning = true;
            wifiScanManager.startScan(scannedDevices -> {
                // Called when scanning completes
                FirebaseRepo repo = new FirebaseRepo();
                for (WiFiDevice device : scannedDevices) {
                    repo.saveDevice(device);
                }
            });
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        wifiScanManager.stopScan();
        isScanning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We are not using bound services, so return null
        return null;
    }

    private void createNotificationChannel() {
        // For newer Android versions, you must create a NotificationChannel
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "WiFi Scan Service",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Scans WiFi in the background.");

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("ForegroundServiceType")
    private void startForegroundServiceNotification() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WiFi Scan Service")
                .setContentText("Scanning WiFi in the background...")
                .setSmallIcon(R.drawable.wifi_location_icon)
                .build();

        startForeground(1, notification);
    }
}