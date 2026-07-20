package com.example.partitioncleaner;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class JunkAdapter extends RecyclerView.Adapter<JunkAdapter.VH> {

    private final List<JunkItem> data;

    public JunkAdapter(List<JunkItem> data) {
        this.data = data;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_junk, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        JunkItem it = data.get(pos);
        h.cb.setOnCheckedChangeListener(null);
        h.cb.setChecked(it.selected);
        h.cb.setText(it.path);
        h.cb.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> it.selected = checked);
        h.tvType.setText(it.typeLabel());
        h.tvSize.setText(Util.formatSize(it.size));

        if (it.advice == JunkItem.ADVICE_KEEP) {
            h.tvAdvice.setText("⚠ " + it.adviceLabel() + "：" + it.reason);
            h.tvAdvice.setTextColor(ContextCompat.getColor(h.tvAdvice.getContext(), R.color.advice_keep));
        } else {
            h.tvAdvice.setText("✓ " + it.adviceLabel() + "：" + it.reason);
            h.tvAdvice.setTextColor(ContextCompat.getColor(h.tvAdvice.getContext(), R.color.advice_clean));
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cb;
        TextView tvType, tvSize, tvAdvice;

        VH(View v) {
            super(v);
            cb = v.findViewById(R.id.cb_select);
            tvType = v.findViewById(R.id.tv_type);
            tvSize = v.findViewById(R.id.tv_size);
            tvAdvice = v.findViewById(R.id.tv_advice);
        }
    }
}
