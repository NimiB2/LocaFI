package dev.nimrod.locafi.maps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.utils.SignalStrengthHelper;

public class WifiMapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "WifiMapFragment";
    private static final float DEFAULT_ZOOM = 15f;
    private static final float DETAIL_ZOOM = 25f;
    private Marker estimatedLocationMarker;
    private GoogleMap mMap;
    private List<WiFiDevice> wifiDevices;
    private Map<String, Circle> deviceCircles = new HashMap<>();
    private Map<String, Marker> deviceMarkers = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wifi_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        updateMapWithDevices();
    }

    public void updateWiFiDevices(List<WiFiDevice> devices) {
        this.wifiDevices = devices;
        if (mMap != null) {
            updateMapWithDevices();
        }
    }

    public void zoomToDevice(WiFiDevice device) {
        if (mMap != null && device.getLatitude() != null && device.getLongitude() != null) {
            LatLng position = new LatLng(device.getLatitude(), device.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, DETAIL_ZOOM));
        }
    }

    public void zoomToLocation(LatLng location) {
        if (mMap != null) {
            // Clear any previous estimated location marker
            if (estimatedLocationMarker != null) {
                estimatedLocationMarker.remove();
            }

            // Create a distinctive marker for estimated location
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location)
                    .title("Your Estimated Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)) // Blue marker
                    .zIndex(2.0f) // Put it on top of other markers
                    .flat(false); // This makes the marker stand upright

            // Add the marker
            estimatedLocationMarker = mMap.addMarker(markerOptions);
            if (estimatedLocationMarker != null) {
                estimatedLocationMarker.showInfoWindow(); // Show the title immediately
            }

            // Set listener to keep info window visible
            mMap.setOnCameraMoveStartedListener(reason -> {
                if (estimatedLocationMarker != null) {
                    estimatedLocationMarker.showInfoWindow();
                }
            });

            // Zoom to location
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DETAIL_ZOOM));
        }
    }

    private void updateMapWithDevices() {
        if (mMap == null || wifiDevices == null) return;

        // Clear existing markers and circles
        clearMarkersAndCircles();

        LatLng defaultLocation = new LatLng(37.422131, -122.084801);
        boolean hasValidDevice = false;

        for (WiFiDevice device : wifiDevices) {
            if (device.getLatitude() != null && device.getLongitude() != null) {
                LatLng position = new LatLng(device.getLatitude(), device.getLongitude());
                int color = SignalStrengthHelper.getColorForSignalStrength(device.getSignalStrength());
                float radius = SignalStrengthHelper.getCircleRadius(device.getSignalStrength());

                // Add circle
                Circle circle = mMap.addCircle(new CircleOptions()
                        .center(position)
                        .radius(radius)
                        .strokeColor(color)
                        .fillColor(color & 0x40FFFFFF) // 25% opacity
                        .strokeWidth(2));
                deviceCircles.put(device.getBssid(), circle);

                // Add colored marker
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .title(device.getSsid())
                        .snippet("Signal: " + device.getSignalStrength() + " dBm")
                        .icon(getBitmapDescriptor(color)));
                if (marker != null) {
                    deviceMarkers.put(device.getBssid(), marker);
                }

                if (!hasValidDevice) {
                    defaultLocation = position;
                    hasValidDevice = true;
                }
            }
        }

        if (hasValidDevice) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM));
        }
    }

    private void clearMarkersAndCircles() {
        for (Circle circle : deviceCircles.values()) {
            circle.remove();
        }
        deviceCircles.clear();

        for (Marker marker : deviceMarkers.values()) {
            marker.remove();
        }
        deviceMarkers.clear();

        // Clear estimated location marker if it exists
        if (estimatedLocationMarker != null) {
            estimatedLocationMarker.remove();
            estimatedLocationMarker = null;
        }
    }

    private BitmapDescriptor getBitmapDescriptor(int color) {
        if (getContext() == null) return BitmapDescriptorFactory.defaultMarker();

        Drawable vectorDrawable = requireContext().getDrawable(R.drawable.baseline_location_on_24);
        if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker();

        Drawable wrappedDrawable = DrawableCompat.wrap(vectorDrawable);
        DrawableCompat.setTint(wrappedDrawable, color);

        int width = wrappedDrawable.getIntrinsicWidth();
        int height = wrappedDrawable.getIntrinsicHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        wrappedDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        wrappedDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}