package dev.nimrod.locafi.maps;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WiFiDevice;
import dev.nimrod.locafi.utils.SignalStrengthHelper;

public class WifiMapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "WifiMapFragment";
    private static final float DEFAULT_ZOOM = 18f;
    private static final float DETAIL_ZOOM = 25f;

    private boolean isScanning = false;
    private Marker estimatedLocationMarker;
    private GoogleMap mMap;
    private List<WiFiDevice> wifiDevices;
    private Marker gpsLocationMarker;
    private boolean isGpsLocationVisible = false;
    private Polyline distanceLine;
    private LatLng estimatedLatLng;
    private LatLng gpsLatLng;
    private Map<String, Circle> deviceCircles = new HashMap<>();
    private Map<String, Marker> deviceMarkers = new HashMap<>();
    public enum MapMode {
        HISTORY,    // For MainActivity - shows historical data
        SCANNING    // For ScanningActivity - shows real-time data
    }

    private MapMode currentMode = MapMode.HISTORY;
    private boolean showUserLocation = false;
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

        if (showUserLocation) {
            try {
                mMap.setMyLocationEnabled(true);
            } catch (SecurityException e) {
                Log.e(TAG, "Error enabling user location: " + e.getMessage());
            }
        }
    }

    public void updateWiFiDevices(List<WiFiDevice> devices) {
        this.wifiDevices = devices;
        if (mMap != null) {
            updateMapWithDevices();
        }
    }
    public void setMapMode(MapMode mode) {
        this.currentMode = mode;
    }

    public void setIsScanning(boolean scanning) {
        this.isScanning = scanning;
    }

    public void zoomToGPSLocation(LatLng location) {
        if (mMap != null && location != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DETAIL_ZOOM));
        }
    }

    public void setShowUserLocation(boolean show) {
        this.showUserLocation = show;
        if (mMap != null) {
            try {
                mMap.setMyLocationEnabled(show);
            } catch (SecurityException e) {
                Log.e(TAG, "Error setting user location visibility: " + e.getMessage());
            }
        }
    }
    public void toggleGPSMarker(LatLng location) {
        if (mMap != null) {
            if (gpsLocationMarker != null) {
                // If marker exists, remove it (toggle off)
                gpsLocationMarker.remove();
                gpsLocationMarker = null;
                gpsLatLng = null;
                isGpsLocationVisible = false;
            } else if (location != null) {
                // If no marker and we have location, add it (toggle on)
                addGPSMarker(location);
            }
            updateDistanceLine();
        }
    }

    public void updateGPSLocation(LatLng location) {
        if (mMap != null && location != null) {
            if (gpsLocationMarker == null) {
                addGPSMarker(location);
            } else {
                gpsLocationMarker.setPosition(location);
            }
            if (isScanning) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, DETAIL_ZOOM));
            }
        }
    }

    // Helper method for creating/updating GPS marker
    private void addGPSMarker(LatLng location) {
        gpsLatLng = location;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .title("Your GPS Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .zIndex(3.0f);

        gpsLocationMarker = mMap.addMarker(markerOptions);
        if (gpsLocationMarker != null) {
            gpsLocationMarker.showInfoWindow();
        }
        isGpsLocationVisible = true;
    }

    public void updateDistanceLine() {
        if (mMap != null) {
            // Remove existing line
            if (distanceLine != null) {
                distanceLine.remove();
                distanceLine = null;
            }

            // Draw new line if both points exist and GPS is visible
            if (estimatedLatLng != null && gpsLatLng != null && isGpsLocationVisible) {
                PolylineOptions lineOptions = new PolylineOptions()
                        .add(estimatedLatLng, gpsLatLng)
                        .width(5)
                        .color(Color.RED)
                        .geodesic(true);
                distanceLine = mMap.addPolyline(lineOptions);
            }
        }
    }

    public float calculateDistance() {
        if (estimatedLatLng != null && gpsLatLng != null) {
            Location loc1 = new Location("");
            loc1.setLatitude(estimatedLatLng.latitude);
            loc1.setLongitude(estimatedLatLng.longitude);

            Location loc2 = new Location("");
            loc2.setLatitude(gpsLatLng.latitude);
            loc2.setLongitude(gpsLatLng.longitude);

            return loc1.distanceTo(loc2);
        }
        return -1;
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

            estimatedLatLng = location;

            // Create a distinctive marker for estimated location
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(location)
                    .title("Your Estimated Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    .zIndex(2.0f)
                    .flat(false);

            // Add the marker
            estimatedLocationMarker = mMap.addMarker(markerOptions);
            if (estimatedLocationMarker != null) {
                estimatedLocationMarker.showInfoWindow();
            }

            // Update distance line
            updateDistanceLine();

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

        if (estimatedLocationMarker != null) {
            estimatedLocationMarker.remove();
            estimatedLocationMarker = null;
        }
    }

    public boolean isGpsLocationVisible() {
        return isGpsLocationVisible;
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