package com.example.partitioncleaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ToolboxActivity extends AppCompatActivity {

    static class ToolItem {
        final String title;
        final String sub;
        final String emoji;
        final int tileColor;
        final Class<?> target;

        ToolItem(String title, String sub, String emoji, int tileColor, Class<?> target) {
            this.title = title;
            this.sub = sub;
            this.emoji = emoji;
            this.tileColor = tileColor;
            this.target = target;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_toolbox);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.toolbox_title);

        RecyclerView rv = findViewById(R.id.rv_tools);
        rv.setLayoutManager(new GridLayoutManager(this, 2));

        List<ToolItem> items = new ArrayList<>();
        items.add(new ToolItem(getString(R.string.tool_calc_title), getString(R.string.tool_calc_sub),
                "🧮", R.color.tile_calc, CalculatorActivity.class));
        items.add(new ToolItem(getString(R.string.tool_convert_title), getString(R.string.tool_convert_sub),
                "🔄", R.color.tile_convert, UnitConverterActivity.class));
        items.add(new ToolItem(getString(R.string.tool_device_title), getString(R.string.tool_device_sub),
                "📱", R.color.tile_device, DeviceInfoActivity.class));
        items.add(new ToolItem(getString(R.string.tool_text_title), getString(R.string.tool_text_sub),
                "📝", R.color.tile_text, TextToolsActivity.class));
        items.add(new ToolItem(getString(R.string.tool_random_title), getString(R.string.tool_random_sub),
                "🎲", R.color.tile_random, RandomActivity.class));
        items.add(new ToolItem(getString(R.string.tool_time_title), getString(R.string.tool_time_sub),
                "🕒", R.color.tile_time, TimestampActivity.class));

        rv.setAdapter(new ToolAdapter(items));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    class ToolAdapter extends RecyclerView.Adapter<ToolAdapter.VH> {

        private final List<ToolItem> data;

        ToolAdapter(List<ToolItem> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_tool, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            ToolItem it = data.get(pos);
            h.title.setText(it.title);
            h.sub.setText(it.sub);
            h.emoji.setText(it.emoji);
            h.tile.setBackgroundColor(ContextCompat.getColor(h.tile.getContext(), it.tileColor));
            h.itemView.setOnClickListener(v -> startActivity(new Intent(ToolboxActivity.this, it.target)));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title, sub, emoji;
            LinearLayout tile;

            VH(View v) {
                super(v);
                title = v.findViewById(R.id.tv_tool_title);
                sub = v.findViewById(R.id.tv_tool_sub);
                emoji = v.findViewById(R.id.tv_tool_emoji);
                tile = v.findViewById(R.id.tile_tool);
            }
        }
    }
}
