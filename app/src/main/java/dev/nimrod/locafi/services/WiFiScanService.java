package dev.nimrod.locafi.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.managers.WiFiScanManager;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.utils.FirebaseRepo;

public class WiFiScanService extends Service {
    private static final String CHANNEL_ID = "WIFI_SCAN_CHANNEL";
    public static final String ACTION_STOP_SERVICE = "stop_service";
    public static final String SCAN_RESULTS_UPDATE = "scan_results_update";
    private static final int SCAN_INTERVAL = 10000; // 10 seconds

    private WiFiScanManager wifiScanManager;
    private boolean isScanning = false;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private final FirebaseRepo firebaseRepo = new FirebaseRepo();

    private final Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            if (isScanning) {
                performScan();
                // Schedule next scan
                scanHandler.postDelayed(this, SCAN_INTERVAL);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        wifiScanManager = new WiFiScanManager(this);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP_SERVICE.equals(intent.getAction())) {
            stopService();
            return START_NOT_STICKY;
        }

        if (!isScanning) {
            isScanning = true;
            startForegroundServiceNotification();
            // Start periodic scanning
            scanHandler.post(scanRunnable);
        }
        return START_STICKY;
    }

    private void performScan() {
        wifiScanManager.startScan(scannedDevices -> {
            // Save to Firebase
            for (WiFiDevice device : scannedDevices) {
                device.setTimestamp(System.currentTimeMillis());
                firebaseRepo.saveDevice(device);
            }
            // Broadcast update to UI
            Intent updateIntent = new Intent(SCAN_RESULTS_UPDATE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);
        });
    }

    private void stopService() {
        isScanning = false;
        scanHandler.removeCallbacks(scanRunnable);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isScanning = false;
        scanHandler.removeCallbacks(scanRunnable);
        wifiScanManager.stopScan();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
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
        // Create an intent for stopping the service
        Intent stopIntent = new Intent(this, WiFiScanService.class);
        stopIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                0,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("WiFi Scan Service")
                .setContentText("Scanning WiFi in the background...")
                .setSmallIcon(R.drawable.wifi_location_icon)
                .addAction(android.R.drawable.ic_media_pause, "Stop Scanning", stopPendingIntent)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }
}