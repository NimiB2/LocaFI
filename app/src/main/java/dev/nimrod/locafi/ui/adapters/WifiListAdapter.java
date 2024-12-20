package dev.nimrod.locafi.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import dev.nimrod.locafi.R;
import dev.nimrod.locafi.models.WifiPoint;

public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.WifiViewHolder> {
    private List<WifiPoint> wifiList; // Changed to WifiPoint
    private OnWifiPointClickListener listener;

    public WifiListAdapter(List<WifiPoint> wifiList) {
        this.wifiList = wifiList;
    }

    // Updated interface to use WifiPoint
    public interface OnWifiPointClickListener {
        void onWifiPointClick(WifiPoint wifiPoint);
    }

    public void setOnWifiPointClickListener(OnWifiPointClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WifiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wifi_point, parent, false);
        return new WifiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WifiViewHolder holder, int position) {
        WifiPoint wifiPoint = wifiList.get(position); // Updated to WifiPoint
        holder.bind(wifiPoint, listener);
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public void updateData(List<WifiPoint> newWifiList) {
        this.wifiList = newWifiList;
        notifyDataSetChanged();
    }

    static class WifiViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView nameTextView;
        private final MaterialTextView locationTextView;
        private final MaterialTextView signalTextView;
        private final MaterialTextView latTextView;
        private final MaterialTextView lonTextView;
        private final LinearProgressIndicator signalProgressBar;

        public WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.stored_wifi_MTV_name);
            locationTextView = itemView.findViewById(R.id.stored_wifi_MTV_location);
            signalTextView = itemView.findViewById(R.id.stored_wifi_MTV_signal);
            latTextView = itemView.findViewById(R.id.stored_wifi_MTV_lat);
            lonTextView = itemView.findViewById(R.id.stored_wifi_MTV_lon);
            signalProgressBar = itemView.findViewById(R.id.stored_wifi_progress_signal);
        }

        public void bind(final WifiPoint wifiPoint, final OnWifiPointClickListener listener) {
            // WiFi Name
            String name = wifiPoint.getSsid().isEmpty() ? "Unknown Network" : wifiPoint.getSsid();
            nameTextView.setText("WiFi Name: " + name);

            // Display location data (BSSID)
            locationTextView.setText("Location: " + (wifiPoint.getBssid() != null ? wifiPoint.getBssid() : "Unknown"));

            // Latitude and Longitude
            double latitude = wifiPoint.getLatitude();
            double longitude = wifiPoint.getLongitude();
            latTextView.setText(String.format("Latitude: %.4f", latitude));
            lonTextView.setText(String.format("Longitude: %.4f", longitude));

            // Signal Strength and Classification
            int signalLevel = wifiPoint.getSignalLevel();
            String classification = getSignalClassification(signalLevel);
            signalTextView.setText(String.format("Signal Strength: %d dBm (%s)", wifiPoint.getRssi(), classification));
            signalProgressBar.setProgress(signalLevel);

            // Signal Color Mapping
            String signalColor = wifiPoint.getSignalColor(); // Returns hex color string
            signalProgressBar.setIndicatorColor(Color.parseColor(signalColor));

            // Add Click Listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onWifiPointClick(wifiPoint); // No error now
                }
            });
        }
        private String getSignalClassification(int signalLevel) {
            switch (signalLevel) {
                case 4:
                    return "Strong";
                case 3:
                    return "Good";
                case 2:
                    return "Fair";
                case 1:
                    return "Poor";
                default:
                    return "Very Poor";
            }
        }
    }

}