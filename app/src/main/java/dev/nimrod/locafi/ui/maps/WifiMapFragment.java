package dev.nimrod.locafi.maps;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.List;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WiFiDevice;

public class WifiMapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "WifiMapFragment";

    private GoogleMap mMap;
    private List<WiFiDevice> wifiDevices;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate your XML with the <fragment> inside
        return inflater.inflate(R.layout.fragment_wifi_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve the existing map fragment from the layout
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);  // Initialize map async
        } else {
            Log.e(TAG, "Error: Could not find the map fragment");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Log.d(TAG, "onMapReady called");
        mMap = googleMap;

        // Enable map UI features
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // If you already have a list of WiFi devices, you can update them:
        updateMapWithDevices();
    }

    public void updateWiFiDevices(List<WiFiDevice> devices) {
        Log.d(TAG, "updateWiFiDevices called with "
                + (devices != null ? devices.size() : 0) + " devices");
        this.wifiDevices = devices;
        // If the map is ready, plot them
        if (mMap != null) {
            updateMapWithDevices();
        }
    }

    private void updateMapWithDevices() {
        if (mMap == null || wifiDevices == null) {
            return;
        }
        mMap.clear();

        // Default location example
        LatLng defaultLocation = new LatLng(37.422131, -122.084801);
        boolean hasValidDevice = false;

        for (WiFiDevice device : wifiDevices) {
            if (device.getLatitude() != null && device.getLongitude() != null) {
                LatLng position = new LatLng(device.getLatitude(), device.getLongitude());
                mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(device.getSsid())
                        .snippet("Signal: " + device.getSignalStrength() + " dBm"));
                if (!hasValidDevice) {
                    defaultLocation = position;
                    hasValidDevice = true;
                }
            }
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));
    }
}
