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

    /** 点击某项时回调（用于弹窗展示详情）。 */
    interface OnItemClick {
        void onItemClick(JunkItem item);
    }

    /** 任一项勾选状态变化时回调（用于实时刷新“已选大小”）。 */
    interface OnSelectionChanged {
        void onChanged();
    }

    /** 长按某项时回调（用于打开/跳转文件）。返回 true 表示已消费事件。 */
    interface OnItemLongClick {
        boolean onLongClick(JunkItem item);
    }

    private final List<JunkItem> data;
    private OnItemClick onItemClick;
    private OnSelectionChanged onSelectionChanged;
    private OnItemLongClick onItemLongClick;

    public JunkAdapter(List<JunkItem> data) {
        this.data = data;
    }

    public void setOnItemClick(OnItemClick l) {
        this.onItemClick = l;
    }

    public void setOnSelectionChanged(OnSelectionChanged l) {
        this.onSelectionChanged = l;
    }

    public void setOnItemLongClick(OnItemLongClick l) {
        this.onItemLongClick = l;
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
        final JunkItem item = it;
        h.itemView.setOnClickListener(v -> {
            if (onItemClick != null) onItemClick.onItemClick(item);
        });
        h.itemView.setOnLongClickListener(v -> {
            if (onItemLongClick != null) return onItemLongClick.onLongClick(item);
            return false;
        });
        h.cb.setOnCheckedChangeListener(null);
        h.cb.setChecked(it.selected);
        h.cb.setText(it.path);
        h.cb.setOnCheckedChangeListener((CompoundButton b, boolean checked) -> {
            it.selected = checked;
            if (onSelectionChanged != null) onSelectionChanged.onChanged();
        });
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
