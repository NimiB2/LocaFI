package dev.nimrod.locafi.ui.adapters;

import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.List;

import dev.nimrod.locafi.R;

public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.WifiViewHolder> {
    private List<ScanResult> wifiList;

    public WifiListAdapter(List<ScanResult> wifiList) {
        this.wifiList = wifiList;
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
        ScanResult wifiPoint = wifiList.get(position);
        holder.bind(wifiPoint);
    }

    @Override
    public int getItemCount() {
        return wifiList.size();
    }

    public void updateData(List<ScanResult> newWifiList) {
        this.wifiList = newWifiList;
        notifyDataSetChanged();
    }

    static class WifiViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView nameTextView;
        private final MaterialTextView locationTextView;

        public WifiViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.stored_wifi_MTV_name);
            locationTextView = itemView.findViewById(R.id.stored_wifi_MTV_location);
        }

        public void bind(final ScanResult wifiPoint) {
            nameTextView.setText(wifiPoint.SSID);
            locationTextView.setText(String.format("Signal: %d dBm", wifiPoint.level));
        }
    }
}