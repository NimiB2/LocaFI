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
import dev.nimrod.locafi.utils.SignalStrengthHelper;

public class WiFiDevicesAdapter extends RecyclerView.Adapter<WiFiDevicesAdapter.ViewHolder> {
    private List<WiFiDevice> devices;
    private OnWiFiDeviceClickListener listener;

    public interface OnWiFiDeviceClickListener {
        void onWiFiDeviceClick(WiFiDevice device);
    }

    public WiFiDevicesAdapter(List<WiFiDevice> devices) {
        this.devices = devices;
    }

    public void setOnWiFiDeviceClickListener(OnWiFiDeviceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_stored_wifi_device, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WiFiDevice device = devices.get(position);

        holder.nameTextView.setText("WiFi: " + device.getSsid());
        Double lat = device.getLatitude();
        Double lon = device.getLongitude();
        String loc = (lat != null && lon != null) ? "(" + lat + ", " + lon + ")" : "Unknown";
        holder.locationTextView.setText("Location: " + loc);

        holder.signalTextView.setText("Signal Strength: " + device.getSignalStrength());
        holder.signalProgress.setProgress(SignalStrengthHelper.calculateSignalLevel(device.getSignalStrength()));

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onWiFiDeviceClick(device);
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices != null ? devices.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView locationTextView;
        TextView signalTextView;
        LinearProgressIndicator signalProgress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.stored_wifi_MTV_name);
            locationTextView = itemView.findViewById(R.id.stored_wifi_MTV_location);
            signalTextView = itemView.findViewById(R.id.stored_wifi_MTV_signal);
            signalProgress = itemView.findViewById(R.id.stored_wifi_progress_signal);
        }
    }
}