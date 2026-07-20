package com.example.partitioncleaner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PartitionAdapter extends RecyclerView.Adapter<PartitionAdapter.VH> {

    private final List<PartitionInfo> data;

    public PartitionAdapter(List<PartitionInfo> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_partition, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        PartitionInfo p = data.get(pos);
        h.tvName.setText(p.name);
        h.tvMount.setText(p.mountPoint);
        h.tvDetail.setText("已用 " + Util.formatSize(p.getUsed())
                + " / 可用 " + Util.formatSize(p.free)
                + " / 总 " + Util.formatSize(p.total));
        int percent = p.getPercent();
        h.pb.setProgress(percent);
        h.tvPercent.setText(percent + "%");
        int usageColor = percent >= 85 ? R.color.usage_high
                : (percent >= 60 ? R.color.usage_mid : R.color.usage_low);
        h.pb.setProgressTintList(ContextCompat.getColorStateList(h.pb.getContext(), usageColor));
        h.tvPercent.setTextColor(ContextCompat.getColor(h.tvPercent.getContext(), usageColor));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvMount, tvDetail, tvPercent;
        ProgressBar pb;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_name);
            tvMount = v.findViewById(R.id.tv_mount);
            tvDetail = v.findViewById(R.id.tv_detail);
            tvPercent = v.findViewById(R.id.tv_percent);
            pb = v.findViewById(R.id.pb_partition);
        }
    }
}
