package dev.nimrod.locafi.utils;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import dev.nimrod.locafi.models.WiFiDevice;

public class FirebaseRepo {

    private static final String TAG = "FirebaseRepo";
    private final FirebaseFirestore db;
    private Firebase firebase;

    public FirebaseRepo() {
        db = FirebaseFirestore.getInstance(); // or Realtime Database if you prefer
    }

    public void saveDevice(WiFiDevice wifiDevice) {
        if (wifiDevice.getBssid() == null || wifiDevice.getBssid().isEmpty()) {
            return;
        }
        db.collection("wifiDevices")
                .document(wifiDevice.getBssid())
                .set(wifiDevice)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Device saved");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error saving device", e);
                    }
                });
    }

    public void getAllDevices(final GetAllDevicesCallback callback) {
        db.collection("wifiDevices")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<WiFiDevice> list = queryDocumentSnapshots.toObjects(WiFiDevice.class);
                    if (callback != null) {
                        callback.onComplete(list);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error getting devices", e);
                    if (callback != null) {
                        callback.onComplete(null);
                    }
                });
    }

    public void clearAllDevices(final ClearAllDevicesCallback callback) {
        db.collection("wifiDevices")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    db.runBatch(batch -> {
                        queryDocumentSnapshots.forEach(doc -> batch.delete(doc.getReference()));
                    }).addOnSuccessListener(aVoid -> {
                        if (callback != null) {
                            callback.onComplete(true);
                        }
                    }).addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onComplete(false);
                        }
                    });
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


    // Callback interface for asynchronous getAllDevices
    public interface GetAllDevicesCallback {
        void onComplete(List<WiFiDevice> devices);
    }
}