package com.example.partitioncleaner;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private FrameLayout container;
    private BottomNavigationView nav;
    private int currentTab = 0;
    private boolean signed = false;

    private static final String REPO_URL = "https://github.com/Johnny520/partition-cleaner";
    private static final String RELEASES_URL = "https://github.com/Johnny520/partition-cleaner/releases";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        container = findViewById(R.id.fl_container);
        nav = findViewById(R.id.nav_view);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_nav) showTab(0);
            else if (id == R.id.nav_home) showTab(1);
            else showTab(2);
            return true;
        });

        showTab(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_ai) {
            startActivity(new Intent(this, AiAssistantActivity.class));
            return true;
        } else if (id == R.id.action_theme) {
            startActivity(new Intent(this, ThemePickerActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showTab(int tab) {
        currentTab = tab;
        container.removeAllViews();
        LayoutInflater inf = getLayoutInflater();
        if (tab == 0) {
            inf.inflate(R.layout.fragment_nav, container, true);
            setupNav();
            setTitle(R.string.title_nav);
        } else if (tab == 1) {
            inf.inflate(R.layout.fragment_home, container, true);
            setupHome();
            setTitle(R.string.title_home);
        } else {
            inf.inflate(R.layout.fragment_user, container, true);
            setupUser();
            setTitle(R.string.title_user);
        }
    }

    /* ===================== 导航 Tab ===================== */
    private void setupNav() {
        RecyclerView rv = container.findViewById(R.id.rv_nav);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<NavItem> items = new ArrayList<>();

        items.add(NavItem.header(getString(R.string.cat_clean)));
        items.add(new NavItem("🧹", getString(R.string.feat_junk), getString(R.string.feat_junk_sub), () -> startScan(false)));
        items.add(new NavItem("📁", getString(R.string.feat_empty), getString(R.string.feat_empty_sub), () -> startScan(true)));
        items.add(new NavItem("📑", getString(R.string.feat_dup), getString(R.string.feat_dup_sub), () -> go(DuplicateActivity.class)));
        items.add(new NavItem("💽", getString(R.string.feat_partition), getString(R.string.feat_partition_sub), () -> go(PartitionActivity.class)));

        items.add(NavItem.header(getString(R.string.cat_tools)));
        items.add(new NavItem("🧮", getString(R.string.tool_calc_title), getString(R.string.tool_calc_sub), () -> go(CalculatorActivity.class)));
        items.add(new NavItem("🔄", getString(R.string.tool_convert_title), getString(R.string.tool_convert_sub), () -> go(UnitConverterActivity.class)));
        items.add(new NavItem("📱", getString(R.string.tool_device_title), getString(R.string.tool_device_sub), () -> go(DeviceInfoActivity.class)));
        items.add(new NavItem("📝", getString(R.string.tool_text_title), getString(R.string.tool_text_sub), () -> go(TextToolsActivity.class)));
        items.add(new NavItem("🎲", getString(R.string.tool_random_title), getString(R.string.tool_random_sub), () -> go(RandomActivity.class)));
        items.add(new NavItem("🕒", getString(R.string.tool_time_title), getString(R.string.tool_time_sub), () -> go(TimestampActivity.class)));

        items.add(NavItem.header(getString(R.string.cat_smart)));
        items.add(new NavItem("✨", getString(R.string.feat_ai), getString(R.string.feat_ai_sub), () -> go(AiAssistantActivity.class)));
        items.add(new NavItem("🎨", getString(R.string.feat_theme), getString(R.string.feat_theme_sub), () -> go(ThemePickerActivity.class)));
        items.add(new NavItem("ℹ️", getString(R.string.feat_about), getString(R.string.feat_about_sub), () -> go(AboutActivity.class)));

        rv.setAdapter(new NavAdapter(items));
    }

    static class NavItem {
        final boolean isHeader;
        final String label;
        final String emoji;
        final String title;
        final String sub;
        final Runnable action;

        private NavItem(boolean h, String label, String emoji, String title, String sub, Runnable action) {
            this.isHeader = h;
            this.label = label;
            this.emoji = emoji;
            this.title = title;
            this.sub = sub;
            this.action = action;
        }

        static NavItem header(String label) {
            return new NavItem(true, label, null, null, null, null);
        }

        NavItem(String emoji, String title, String sub, Runnable action) {
            this(false, null, emoji, title, sub, action);
        }
    }

    class NavAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_FEATURE = 1;
        private final List<NavItem> data;

        NavAdapter(List<NavItem> data) { this.data = data; }

        @Override
        public int getItemViewType(int pos) {
            return data.get(pos).isHeader ? TYPE_HEADER : TYPE_FEATURE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_nav_header, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_feature, parent, false);
            return new FeatureVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
            NavItem it = data.get(pos);
            if (it.isHeader) {
                ((TextView) h.itemView).setText(it.label);
                return;
            }
            FeatureVH vh = (FeatureVH) h;
            vh.emoji.setText(it.emoji);
            vh.title.setText(it.title);
            vh.sub.setText(it.sub);
            vh.itemView.setOnClickListener(v -> it.action.run());
        }

        @Override
        public int getItemCount() { return data.size(); }

        class FeatureVH extends RecyclerView.ViewHolder {
            TextView emoji, title, sub;
            FeatureVH(View v) {
                super(v);
                emoji = v.findViewById(R.id.tv_feat_emoji);
                title = v.findViewById(R.id.tv_feat_title);
                sub = v.findViewById(R.id.tv_feat_sub);
            }
        }
    }

    /* ===================== 主页 Tab ===================== */
    private void setupHome() {
        container.findViewById(R.id.btn_home_scan).setOnClickListener(v -> startScan(false));
        container.findViewById(R.id.btn_home_refresh).setOnClickListener(v -> loadDashboard());
        loadDashboard();
    }

    private void loadDashboard() {
        new Thread(() -> {
            // 存储
            long storageTotal = 0, storageAvail = 0;
            try {
                StatFs stat = new StatFs(getFilesDir().getAbsolutePath());
                if (Build.VERSION.SDK_INT >= 18) {
                    storageTotal = stat.getTotalBytes();
                    storageAvail = stat.getAvailableBytes();
                } else {
                    storageTotal = (long) stat.getBlockCount() * stat.getBlockSize();
                    storageAvail = (long) stat.getAvailableBlocks() * stat.getBlockSize();
                }
            } catch (Exception ignored) {}
            long storageUsed = storageTotal - storageAvail;
            int storagePct = storageTotal > 0 ? (int) (storageUsed * 100 / storageTotal) : 0;

            // 内存
            long ramTotal = 0, ramAvail = 0;
            try {
                ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                ramTotal = mi.totalMem;
                ramAvail = mi.availMem;
            } catch (Exception ignored) {}
            long ramUsed = ramTotal - ramAvail;
            int ramPct = ramTotal > 0 ? (int) (ramUsed * 100 / ramTotal) : 0;

            // 电池
            int battLevel = -1, battTemp = 0, battHealth = 0, battStatus = 0;
            try {
                Intent batt = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batt != null) {
                    int level = batt.getIntExtra("level", -1);
                    int scale = batt.getIntExtra("scale", -1);
                    battLevel = scale > 0 ? Math.round(level * 100f / scale) : level;
                    battTemp = batt.getIntExtra("temperature", 0) / 10;
                    battHealth = batt.getIntExtra("health", 0);
                    battStatus = batt.getIntExtra("status", 0);
                }
            } catch (Exception ignored) {}

            // CPU
            int cores = Runtime.getRuntime().availableProcessors();
            String freq = readCpuFreq();
            String cpuModel = safe(Build.HARDWARE) + " · " + safe(Build.BOARD);

            // 传感器 / 应用
            int sensors = 0, apps = 0;
            try {
                SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
                sensors = sm.getSensorList(android.hardware.Sensor.TYPE_ALL).size();
            } catch (Exception ignored) {}
            try {
                apps = getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA).size();
            } catch (Exception ignored) {}

            final int fStoragePct = storagePct, fRamPct = ramPct, fBattLevel = battLevel,
                    fBattTemp = battTemp, fBattHealth = battHealth, fBattStatus = battStatus,
                    fCores = cores, fSensors = sensors, fApps = apps;
            final long fStorageUsed = storageUsed, fStorageTotal = storageTotal,
                    fRamUsed = ramUsed, fRamTotal = ramTotal;
            final String fFreq = freq, fCpuModel = cpuModel;

            runOnUiThread(() -> {
                setText(R.id.tv_storage_detail, getString(R.string.home_used) + " " + Util.formatSize(fStorageUsed)
                        + " / " + getString(R.string.home_total) + " " + Util.formatSize(fStorageTotal));
                setText(R.id.tv_storage_pct, fStoragePct + "%");
                setBar(R.id.pb_storage, fStoragePct);

                setText(R.id.tv_ram_detail, getString(R.string.home_used) + " " + Util.formatSize(fRamUsed)
                        + " / " + getString(R.string.home_total) + " " + Util.formatSize(fRamTotal));
                setText(R.id.tv_ram_pct, fRamPct + "%");
                setBar(R.id.pb_ram, fRamPct);

                setText(R.id.tv_battery_level, fBattLevel >= 0 ? fBattLevel + "%" : "—");
                setText(R.id.tv_battery_temp, fBattTemp + "°C");
                setText(R.id.tv_battery_health, healthText(fBattHealth));
                if (fBattStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    setText(R.id.tv_battery_level, (fBattLevel >= 0 ? fBattLevel + "%" : "—") + " ⚡");
                }

                setText(R.id.tv_cpu_cores, String.valueOf(fCores));
                setText(R.id.tv_cpu_freq, fFreq);
                setText(R.id.tv_cpu_model, fCpuModel);

                setText(R.id.tv_sensors, String.valueOf(fSensors));
                setText(R.id.tv_apps, String.valueOf(fApps));
            });
        }).start();
    }

    private String readCpuFreq() {
        try {
            int cores = Runtime.getRuntime().availableProcessors();
            for (int i = 0; i < cores; i++) {
                File f = new File("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq");
                if (f.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line = br.readLine();
                        if (line != null) {
                            long khz = Long.parseLong(line.trim());
                            return (khz / 1000) + " MHz";
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException ignored) {}
        return "未知";
    }

    private String healthText(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "良好";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "过热";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "失效";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "过压";
            default: return "正常";
        }
    }

    /* ===================== 用户 Tab ===================== */
    private void setupUser() {
        RecyclerView rv = container.findViewById(R.id.rv_user);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<FeatureItem> items = new ArrayList<>();
        items.add(new FeatureItem("✨", getString(R.string.feat_ai), getString(R.string.feat_ai_sub), () -> go(AiAssistantActivity.class)));
        items.add(new FeatureItem("🎨", getString(R.string.feat_theme), getString(R.string.feat_theme_sub), () -> go(ThemePickerActivity.class)));
        items.add(new FeatureItem("ℹ️", getString(R.string.feat_about), getString(R.string.feat_about_sub), () -> go(AboutActivity.class)));
        items.add(new FeatureItem("🔐", getString(R.string.feat_permission), getString(R.string.feat_permission_sub), this::openAppSettings));
        items.add(new FeatureItem("🌐", getString(R.string.feat_website), getString(R.string.feat_website_sub), () -> openUrl(RELEASES_URL)));
        items.add(new FeatureItem("📤", getString(R.string.feat_share), getString(R.string.feat_share_sub), this::shareApp));
        rv.setAdapter(new FeatureAdapter(items));

        com.google.android.material.button.MaterialButton btnSign = container.findViewById(R.id.btn_sign);
        if (signed) {
            btnSign.setText(R.string.user_signed);
            btnSign.setEnabled(false);
        }
        btnSign.setOnClickListener(v -> {
            signed = true;
            btnSign.setText(R.string.user_signed);
            btnSign.setEnabled(false);
            Toast.makeText(this, R.string.user_signed, Toast.LENGTH_SHORT).show();
        });
    }

    static class FeatureItem {
        final String emoji, title, sub;
        final Runnable action;
        FeatureItem(String emoji, String title, String sub, Runnable action) {
            this.emoji = emoji; this.title = title; this.sub = sub; this.action = action;
        }
    }

    class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.VH> {
        private final List<FeatureItem> data;
        FeatureAdapter(List<FeatureItem> data) { this.data = data; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feature, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            FeatureItem it = data.get(pos);
            h.emoji.setText(it.emoji);
            h.title.setText(it.title);
            h.sub.setText(it.sub);
            h.itemView.setOnClickListener(v -> it.action.run());
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView emoji, title, sub;
            VH(View v) {
                super(v);
                emoji = v.findViewById(R.id.tv_feat_emoji);
                title = v.findViewById(R.id.tv_feat_title);
                sub = v.findViewById(R.id.tv_feat_sub);
            }
        }
    }

    /* ===================== 工具方法 ===================== */
    private void go(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    private void startScan(boolean emptyOnly) {
        Intent intent = new Intent(this, ScanResultActivity.class);
        intent.putExtra(ScanResultActivity.EXTRA_EMPTY_ONLY, emptyOnly);
        startActivity(intent);
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开：" + url, Toast.LENGTH_LONG).show();
        }
    }

    private void openAppSettings() {
        try {
            Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开权限设置", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApp() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, "分区清理大师 - " + REPO_URL);
        startActivity(Intent.createChooser(i, getString(R.string.feat_share)));
    }

    private void setText(int id, String s) {
        View v = container.findViewById(id);
        if (v instanceof TextView) ((TextView) v).setText(s);
    }

    private void setBar(int id, int p) {
        View v = container.findViewById(id);
        if (v instanceof LinearProgressIndicator) ((LinearProgressIndicator) v).setProgress(p);
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "未知" : s;
    }
}
