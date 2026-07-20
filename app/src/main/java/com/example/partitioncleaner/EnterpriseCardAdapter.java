package com.example.partitioncleaner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/** 企业查询结果的原生卡片适配器（数据来自国家企业信用公示系统）。 */
public class EnterpriseCardAdapter extends RecyclerView.Adapter<EnterpriseCardAdapter.VH> {

    public interface OnItemClick {
        void onItemClick(EnterpriseActivity.EntItem item);
    }

    private final List<EnterpriseActivity.EntItem> items;
    private final OnItemClick listener;

    public EnterpriseCardAdapter(List<EnterpriseActivity.EntItem> items, OnItemClick listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_enterprise_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        EnterpriseActivity.EntItem it = items.get(pos);
        h.name.setText(it.name);
        h.itemView.setOnClickListener(v -> listener.onItemClick(it));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tv_card_name);
        }
    }
}
