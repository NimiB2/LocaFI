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
    private static final String USERS_PATH = "users";
    private static final String WIFI_DEVICES_PATH = "wifiDevices";
    private final DatabaseReference userDbRef;
    private final String userId;

    public FirebaseRepo(String userId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.userId = userId;
        this.userDbRef = database.getReference(USERS_PATH).child(userId).child(WIFI_DEVICES_PATH);
    }

    public void saveDevice(WiFiDevice wifiDevice) {
        if (wifiDevice.getBssid() == null || wifiDevice.getBssid().isEmpty()) {
            Log.e(TAG, "Cannot save device: BSSID is null or empty");
            return;
        }
        DatabaseReference deviceRef = userDbRef.child(wifiDevice.getBssid());

        deviceRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                WiFiDevice existingDevice = snapshot.getValue(WiFiDevice.class);

                if (existingDevice == null || existingDevice.getTimestamp() < wifiDevice.getTimestamp()) {
                    deviceRef.setValue(wifiDevice)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully saved device: " + wifiDevice.getBssid()))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to save device: " + wifiDevice.getBssid(), e));
                }
            } else {
                Log.e(TAG, "Error checking existing device", task.getException());
            }
        });
    }

    public void getAllDevices(final GetAllDevicesCallback callback) {
        userDbRef.addValueEventListener(new ValueEventListener() {
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
        userDbRef.removeValue()
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

    public void addTestData() {
        // Original devices
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

        // New clustered devices around Google Plex
        WiFiDevice device4 = new WiFiDevice();
        device4.setBssid("22:33:44:55:66:77");
        device4.setSsid("GooglePlex_Building_A");
        device4.setSignalStrength(-58);
        device4.setTimestamp(System.currentTimeMillis());
        device4.setLatitude(37.422145);  // Very close to device1
        device4.setLongitude(-122.084810);

        WiFiDevice device5 = new WiFiDevice();
        device5.setBssid("33:44:55:66:77:88");
        device5.setSsid("GooglePlex_Building_B");
        device5.setSignalStrength(-62);
        device5.setTimestamp(System.currentTimeMillis());
        device5.setLatitude(37.422140);  // Very close to device1 and device4
        device5.setLongitude(-122.084795);

        // Clustered devices around Mountain View Cafe
        WiFiDevice device6 = new WiFiDevice();
        device6.setBssid("44:55:66:77:88:99");
        device6.setSsid("MountainView_Shop1");
        device6.setSignalStrength(-70);
        device6.setTimestamp(System.currentTimeMillis());
        device6.setLatitude(37.421990);  // Very close to device2
        device6.setLongitude(-122.083475);

        WiFiDevice device7 = new WiFiDevice();
        device7.setBssid("55:66:77:88:99:AA");
        device7.setSsid("MountainView_Shop2");
        device7.setSignalStrength(-75);
        device7.setTimestamp(System.currentTimeMillis());
        device7.setLatitude(37.421982);  // Very close to device2 and device6
        device7.setLongitude(-122.083460);

        Log.d(TAG, "Starting to add test data...");

        saveDevice(device1);
        saveDevice(device2);
        saveDevice(device3);
        saveDevice(device4);
        saveDevice(device5);
        saveDevice(device6);
        saveDevice(device7);

        Log.d(TAG, "Finished adding test data");
    }

    public interface ClearAllDevicesCallback {
        void onComplete(boolean success);
    }

    public interface GetAllDevicesCallback {
        void onComplete(List<WiFiDevice> devices);
    }
}