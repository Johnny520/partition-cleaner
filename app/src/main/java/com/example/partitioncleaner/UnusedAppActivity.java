package com.example.partitioncleaner;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 长期未使用的 APP：按“最近更新时间”升序排列，越久没动过的越靠前，
 * 方便用户清理掉不再使用的应用。点击“打开”进入系统应用详情页。
 */
public class UnusedAppActivity extends AppCompatActivity {

    static class AppEntry {
        String label;
        String pkg;
        long lastUpdate;
        android.graphics.drawable.Drawable icon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unused_app);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        RecyclerView rv = findViewById(R.id.rv_unused);
        rv.setLayoutManager(new LinearLayoutManager(this));
        Adapter adapter = new Adapter();
        rv.setAdapter(adapter);

        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppEntry> list = new ArrayList<>();
            for (ApplicationInfo ai : apps) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue; // 仅看用户应用
                AppEntry e = new AppEntry();
                e.label = ai.loadLabel(pm).toString();
                e.pkg = ai.packageName;
                e.icon = ai.loadIcon(pm);
                try {
                    PackageInfo pi = pm.getPackageInfo(ai.packageName, 0);
                    e.lastUpdate = pi.lastUpdateTime;
                } catch (Exception ignored) {
                    e.lastUpdate = 0;
                }
                list.add(e);
            }
            Collections.sort(list, (a, b) -> Long.compare(a.lastUpdate, b.lastUpdate));
            final List<AppEntry> sorted = list;
            runOnUiThread(() -> adapter.setItems(sorted));
        }).start();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private List<AppEntry> items = new ArrayList<>();

        void setItems(List<AppEntry> l) {
            items = l;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_unused, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppEntry e = items.get(pos);
            if (e.icon != null) h.icon.setImageDrawable(e.icon);
            h.name.setText(e.label);
            h.pkg.setText(e.pkg);
            h.extra.setText(getString(R.string.unused_last_update) + formatTime(e.lastUpdate));
            h.btn.setOnClickListener(v -> openAppSettings(e.pkg));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView icon;
       TextView name, pkg, extra;
            Button btn;

            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.iv_icon);
                name = v.findViewById(R.id.tv_name);
                pkg = v.findViewById(R.id.tv_pkg);
                extra = v.findViewById(R.id.tv_extra);
                btn = v.findViewById(R.id.btn_open);
            }
        }
    }

    private void openAppSettings(String pkg) {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + pkg));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, R.string.unused_open_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTime(long t) {
        if (t <= 0) return "未知";
        try {
            return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(t));
        } catch (Exception e) {
            return String.valueOf(t);
        }
    }
}
