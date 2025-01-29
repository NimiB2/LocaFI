package dev.nimrod.locafi.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WiFiDevice;

public class WiFiDevicesAdapter extends RecyclerView.Adapter<WiFiDevicesAdapter.ViewHolder> {

    private List<WiFiDevice> devices;

    public WiFiDevicesAdapter(List<WiFiDevice> devices) {
        this.devices = devices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the new row layout (adjust file name as needed)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_stored_wifi_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WiFiDevice device = devices.get(position);

        // Name
        holder.nameTextView.setText("WiFi: " + device.getSsid());

        // Latitude / Longitude
        holder.latTextView.setText("Latitude: " + device.getLatitude());
        holder.lonTextView.setText("Longitude: " + device.getLongitude());

        // Location
        Double lat = device.getLatitude();
        Double lon = device.getLongitude();
        String loc;
        if (lat != null && lon != null) {
            loc = "(" + lat + ", " + lon + ")";
        } else {
            loc = "Unknown";
        }
        holder.locationTextView.setText("Location: " + loc);


        // Signal Strength label
        holder.signalTextView.setText("Signal Strength: " + device.getSignalStrength());

        // Convert signal level to progress bar value
        int signalLevel = calculateSignalLevel(device.getSignalStrength());
        holder.signalProgress.setProgress(signalLevel);
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    /**
     * Simple signal-level mapping for demonstration.
     * Adjust logic based on your RSSI range and how you want to display signal strength (0..4).
     */
    private int calculateSignalLevel(int rssi) {
        // Example: map approximate ranges of dBm to 0-4
        // dBm is often negative (e.g., -30 is strong, -90 is weak).
        // Tweak as needed for your own scale.
        if (rssi >= -50) {
            return 4; // Excellent
        } else if (rssi >= -60) {
            return 3; // Good
        } else if (rssi >= -70) {
            return 2; // Fair
        } else if (rssi >= -80) {
            return 1; // Poor
        } else {
            return 0; // Very poor
        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView;
        TextView latTextView;
        TextView lonTextView;
        TextView locationTextView;
        TextView signalTextView;
        LinearProgressIndicator signalProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView     = itemView.findViewById(R.id.stored_wifi_MTV_name);
            latTextView      = itemView.findViewById(R.id.stored_wifi_MTV_lat);
            lonTextView      = itemView.findViewById(R.id.stored_wifi_MTV_lon);
            locationTextView = itemView.findViewById(R.id.stored_wifi_MTV_location);
            signalTextView   = itemView.findViewById(R.id.stored_wifi_MTV_signal);
            signalProgress   = itemView.findViewById(R.id.stored_wifi_progress_signal);
        }
    }
}
