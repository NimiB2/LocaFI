package dev.nimrod.locafi.utils;

import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import dev.nimrod.locafi.models.WiFiDevice;

public class FirebaseRepo {
    private static final String TAG = "FirebaseRepo";
    private final DatabaseReference dbRef;

    public FirebaseRepo() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.dbRef = database.getReference("wifiDevices");
    }

    public void saveDevice(WiFiDevice wifiDevice) {
        if (wifiDevice.getBssid() == null || wifiDevice.getBssid().isEmpty()) {
            Log.e(TAG, "Cannot save device: BSSID is null or empty");
            return;
        }
        DatabaseReference deviceRef = dbRef.child(wifiDevice.getBssid());

        deviceRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                WiFiDevice existingDevice = snapshot.getValue(WiFiDevice.class);

                // If device doesn't exist or this is a newer reading
                if (existingDevice == null ||
                        existingDevice.getTimestamp() < wifiDevice.getTimestamp()) {

                    deviceRef.setValue(wifiDevice)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Successfully saved/updated device: " +
                                        wifiDevice.getBssid());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to save device: " +
                                        wifiDevice.getBssid(), e);
                            });
                }
            } else {
                Log.e(TAG, "Error checking existing device", task.getException());
            }
        });
    }
    public void getAllDevices(final GetAllDevicesCallback callback) {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<WiFiDevice> devices = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    WiFiDevice device = snapshot.getValue(WiFiDevice.class);
                    if (device != null) {
                        devices.add(device);
                    }
                }
                if (callback != null) {
                    callback.onComplete(devices);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.w(TAG, "getAllDevices:onCancelled", error.toException());
                if (callback != null) {
                    callback.onComplete(null);
                }
            }
        });
    }

    public void clearAllDevices(final ClearAllDevicesCallback callback) {
        dbRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onComplete(true);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onComplete(false);
                    }
                });
    }

    public interface ClearAllDevicesCallback {
        void onComplete(boolean success);
    }

    public interface GetAllDevicesCallback {
        void onComplete(List<WiFiDevice> devices);
    }

    public void addTestData() {
        // Mountain View area coordinates (near Google HQ - default emulator location)

        WiFiDevice device1 = new WiFiDevice();
        device1.setBssid("00:11:22:33:44:55");
        device1.setSsid("GooglePlex_WiFi");
        device1.setSignalStrength(-65);
        device1.setTimestamp(System.currentTimeMillis());
        device1.setLatitude(37.422131);
        device1.setLongitude(-122.084801);

        WiFiDevice device2 = new WiFiDevice();
        device2.setBssid("AA:BB:CC:DD:EE:FF");
        device2.setSsid("MountainView_Cafe");
        device2.setSignalStrength(-72);
        device2.setTimestamp(System.currentTimeMillis());
        device2.setLatitude(37.421987);
        device2.setLongitude(-122.083468);

        WiFiDevice device3 = new WiFiDevice();
        device3.setBssid("11:22:33:44:55:66");
        device3.setSsid("Castro_Street_WiFi");
        device3.setSignalStrength(-55);
        device3.setTimestamp(System.currentTimeMillis());
        device3.setLatitude(37.423982);
        device3.setLongitude(-122.082421);

        Log.d(TAG, "Starting to add test data...");

        // Save devices and log results
        saveDevice(device1);
        saveDevice(device2);
        saveDevice(device3);

        Log.d(TAG, "Finished adding test data");
    }
}