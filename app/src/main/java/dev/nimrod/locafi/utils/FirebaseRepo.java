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
    private static final String WIFI_DEVICES_PATH = "wifiDevices";

    private final DatabaseReference dbRef;

    public FirebaseRepo() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.dbRef = database.getReference(WIFI_DEVICES_PATH);
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
        // Mountain View area coordinates (near Google HQ)
        WiFiDevice[] devices = {
                createDevice("00:11:22:33:44:55", "GooglePlex_WiFi_1", -65, 37.422131, -122.084801),
                createDevice("AA:BB:CC:DD:EE:FF", "GooglePlex_WiFi_2", -72, 37.422140, -122.084810),
                createDevice("11:22:33:44:55:66", "GooglePlex_WiFi_3", -55, 37.422125, -122.084795),
                createDevice("22:33:44:55:66:77", "Building_A_WiFi", -58, 37.422000, -122.084700),
                createDevice("33:44:55:66:77:88", "Building_B_WiFi", -75, 37.422200, -122.084900),
                createDevice("44:55:66:77:88:99", "Cafe_WiFi_1", -68, 37.421987, -122.083468),
                createDevice("55:66:77:88:99:AA", "Cafe_WiFi_2", -70, 37.421990, -122.083470),
                createDevice("66:77:88:99:AA:BB", "Street_WiFi_1", -62, 37.423982, -122.082421),
                createDevice("77:88:99:AA:BB:CC", "Street_WiFi_2", -80, 37.423985, -122.082425)
        };

        Log.d(TAG, "Starting to add test data...");
        for (WiFiDevice device : devices) {
            saveDevice(device);
        }
        Log.d(TAG, "Finished adding test data");
    }

    private WiFiDevice createDevice(String bssid, String ssid, int strength, double lat, double lon) {
        WiFiDevice device = new WiFiDevice();
        device.setBssid(bssid);
        device.setSsid(ssid);
        device.setSignalStrength(strength);
        device.setTimestamp(System.currentTimeMillis());
        device.setLatitude(lat);
        device.setLongitude(lon);
        return device;
    }
}