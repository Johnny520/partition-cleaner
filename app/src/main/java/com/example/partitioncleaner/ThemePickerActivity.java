package com.example.partitioncleaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

public class ThemePickerActivity extends BaseActivity {

    private int current;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme_picker);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.feat_theme);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        current = ThemeManager.getTheme(this);

        RecyclerView rv = findViewById(R.id.rv_theme);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new ThemeAdapter());
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_theme, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            h.name.setText(ThemeManager.NAMES[pos]);
            h.desc.setText(ThemeManager.DESCS[pos]);
            h.swatch.setCardBackgroundColor(ThemeManager.PREVIEW[pos]);
            h.check.setVisibility(pos == current ? View.VISIBLE : View.GONE);
            h.itemView.setOnClickListener(v -> {
                ThemeManager.setTheme(ThemePickerActivity.this, pos);
                current = pos;
                notifyDataSetChanged();
                recreate();
            });
        }

        @Override
        public int getItemCount() { return ThemeManager.NAMES.length; }

        class VH extends RecyclerView.ViewHolder {
            TextView name, desc, check;
            MaterialCardView swatch;
            VH(View v) {
                super(v);
                name = v.findViewById(R.id.tv_theme_name);
                desc = v.findViewById(R.id.tv_theme_desc);
                swatch = v.findViewById(R.id.swatch);
                check = v.findViewById(R.id.tv_theme_check);
            }
        }
    }
}
