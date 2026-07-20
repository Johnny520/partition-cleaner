/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.app.ActivityManager;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.net.Uri;
import android.app.AlertDialog;
import android.webkit.MimeTypeMap;
import androidx.core.content.FileProvider;
import java.io.File;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

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
            else if (id == R.id.nav_files) showTab(3);
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
        } else if (tab == 2) {
            inf.inflate(R.layout.fragment_user, container, true);
            setupUser();
            setTitle(R.string.title_user);
        } else {
            inf.inflate(R.layout.fragment_files, container, true);
            setupFiles();
            setTitle(R.string.title_files);
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

        items.add(new NavItem("💬", getString(R.string.clean_wechat), getString(R.string.clean_wechat_sub), () -> goClean(AppCleanScanner.TYPE_WECHAT)));
        items.add(new NavItem("🐧", getString(R.string.clean_qq), getString(R.string.clean_qq_sub), () -> goClean(AppCleanScanner.TYPE_QQ)));
        items.add(new NavItem("🎵", getString(R.string.clean_douyin), getString(R.string.clean_douyin_sub), () -> goClean(AppCleanScanner.TYPE_DOUYIN)));
        items.add(new NavItem("🌐", getString(R.string.clean_browser), getString(R.string.clean_browser_sub), () -> goClean(AppCleanScanner.TYPE_BROWSER)));
        items.add(new NavItem("📦", getString(R.string.clean_apk), getString(R.string.clean_apk_sub), () -> goClean(AppCleanScanner.TYPE_APK)));
        items.add(new NavItem("🖼️", getString(R.string.clean_screenshot), getString(R.string.clean_screenshot_sub), () -> goClean(AppCleanScanner.TYPE_SCREENSHOT)));
        items.add(new NavItem("📜", getString(R.string.clean_log), getString(R.string.clean_log_sub), () -> goClean(AppCleanScanner.TYPE_LOG)));
        items.add(new NavItem("🗂️", getString(R.string.clean_temp), getString(R.string.clean_temp_sub), () -> goClean(AppCleanScanner.TYPE_TEMP)));
        items.add(new NavItem("🐘", getString(R.string.clean_large), getString(R.string.clean_large_sub), () -> goClean(AppCleanScanner.TYPE_LARGE)));
        items.add(new NavItem("🗑️", getString(R.string.clean_residual), getString(R.string.clean_residual_sub), () -> goClean(AppCleanScanner.TYPE_RESIDUAL)));
        items.add(new NavItem("🖼", getString(R.string.clean_thumb), getString(R.string.clean_thumb_sub), () -> goClean(AppCleanScanner.TYPE_THUMB)));
        items.add(new NavItem("🚫", getString(R.string.clean_ad), getString(R.string.clean_ad_sub), () -> goClean(AppCleanScanner.TYPE_AD)));
        items.add(new NavItem("🖼️", getString(R.string.clean_photo), getString(R.string.clean_photo_sub), () -> goClean(AppCleanScanner.TYPE_PHOTO)));
        items.add(new NavItem("📚", getString(R.string.clean_ebook), getString(R.string.clean_ebook_sub), () -> goClean(AppCleanScanner.TYPE_EBOOK)));
        items.add(new NavItem("🗄️", getString(R.string.clean_app_data), getString(R.string.clean_app_data_sub), () -> goClean(AppCleanScanner.TYPE_APP_DATA)));
        items.add(new NavItem("🔥", getString(R.string.clean_deep), getString(R.string.clean_deep_sub), () -> goClean(AppCleanScanner.TYPE_DEEP)));
        items.add(new NavItem("📤", getString(R.string.extract_title), getString(R.string.extract_sub), () -> go(ApkExtractActivity.class)));

        items.add(NavItem.header(getString(R.string.cat_tools)));
        items.add(new NavItem("🧮", getString(R.string.tool_calc_title), getString(R.string.tool_calc_sub), () -> go(CalculatorActivity.class)));
        items.add(new NavItem("🔄", getString(R.string.tool_convert_title), getString(R.string.tool_convert_sub), () -> go(UnitConverterActivity.class)));
        items.add(new NavItem("📱", getString(R.string.tool_device_title), getString(R.string.tool_device_sub), () -> go(DeviceInfoActivity.class)));
        items.add(new NavItem("📝", getString(R.string.tool_text_title), getString(R.string.tool_text_sub), () -> go(TextToolsActivity.class)));
        items.add(new NavItem("🎲", getString(R.string.tool_random_title), getString(R.string.tool_random_sub), () -> go(RandomActivity.class)));
        items.add(new NavItem("🕒", getString(R.string.tool_time_title), getString(R.string.tool_time_sub), () -> go(TimestampActivity.class)));

        items.add(NavItem.header(getString(R.string.cat_social)));
        items.add(new NavItem("📰", getString(R.string.clean_weibo), getString(R.string.clean_weibo_sub), () -> goClean(AppCleanScanner.TYPE_WEIBO)));
        items.add(new NavItem("📕", getString(R.string.clean_xhs), getString(R.string.clean_xhs_sub), () -> goClean(AppCleanScanner.TYPE_XHS)));
        items.add(new NavItem("🎬", getString(R.string.clean_kuaishou), getString(R.string.clean_kuaishou_sub), () -> goClean(AppCleanScanner.TYPE_KUAISHOU)));
        items.add(new NavItem("📺", getString(R.string.clean_bilibili), getString(R.string.clean_bilibili_sub), () -> goClean(AppCleanScanner.TYPE_BILIBILI)));
        items.add(new NavItem("🎧", getString(R.string.clean_netease), getString(R.string.clean_netease_sub), () -> goClean(JunkItem.TYPE_WANGYI)));
        items.add(new NavItem("🛒", getString(R.string.clean_taobao), getString(R.string.clean_taobao_sub), () -> goClean(AppCleanScanner.TYPE_TAOBAO)));
        items.add(new NavItem("🟧", getString(R.string.clean_pdd), getString(R.string.clean_pdd_sub), () -> goClean(AppCleanScanner.TYPE_PDD)));

        items.add(NavItem.header(getString(R.string.cat_appsys)));
        items.add(new NavItem("📲", getString(R.string.clean_app_cache_all), getString(R.string.clean_app_cache_all_sub), () -> goClean(JunkItem.TYPE_APP_CACHE_ALL)));
        items.add(new NavItem("⚙️", getString(R.string.clean_system), getString(R.string.clean_system_sub), () -> goClean(JunkItem.TYPE_SYSTEM_JUNK)));
        items.add(new NavItem("🗺️", getString(R.string.clean_map), getString(R.string.clean_map_sub), () -> goClean(JunkItem.TYPE_MAPS)));
        items.add(new NavItem("⌨️", getString(R.string.clean_ime), getString(R.string.clean_ime_sub), () -> goClean(AppCleanScanner.TYPE_IME)));

        items.add(NavItem.header(getString(R.string.cat_file)));
        items.add(new NavItem("🎵", getString(R.string.clean_music), getString(R.string.clean_music_sub), () -> goClean(AppCleanScanner.TYPE_MUSIC)));
        items.add(new NavItem("🎞️", getString(R.string.clean_video), getString(R.string.clean_video_sub), () -> goClean(AppCleanScanner.TYPE_VIDEO_FILE)));
        items.add(new NavItem("📄", getString(R.string.clean_doc), getString(R.string.clean_doc_sub), () -> goClean(AppCleanScanner.TYPE_DOC)));
        items.add(new NavItem("🗜️", getString(R.string.clean_archive), getString(R.string.clean_archive_sub), () -> goClean(AppCleanScanner.TYPE_ARCHIVE)));
        items.add(new NavItem("⬇️", getString(R.string.clean_download), getString(R.string.clean_download_sub), () -> goClean(AppCleanScanner.TYPE_DOWNLOAD)));
        items.add(new NavItem("📄", getString(R.string.clean_empty_file), getString(R.string.clean_empty_file_sub), () -> goClean(AppCleanScanner.TYPE_EMPTY_FILE)));
        items.add(new NavItem("🔵", getString(R.string.clean_bluetooth), getString(R.string.clean_bluetooth_sub), () -> goClean(AppCleanScanner.TYPE_BLUETOOTH)));
        items.add(new NavItem("🎙️", getString(R.string.clean_record), getString(R.string.clean_record_sub), () -> goClean(AppCleanScanner.TYPE_RECORD)));
        items.add(new NavItem("🖼️", getString(R.string.clean_wallpaper), getString(R.string.clean_wallpaper_sub), () -> goClean(AppCleanScanner.TYPE_WALLPAPER)));

        items.add(new NavItem("🛠️", getString(R.string.custom_title), getString(R.string.custom_sub), () -> go(CustomCleanActivity.class)));
        items.add(new NavItem("🚀", getString(R.string.memboost_title), getString(R.string.memboost_sub), () -> go(MemoryBoostActivity.class)));

        items.add(NavItem.header(getString(R.string.cat_smart)));
        items.add(new NavItem("✨", getString(R.string.feat_ai), getString(R.string.feat_ai_sub), () -> go(AiAssistantActivity.class)));
        items.add(new NavItem("🎨", getString(R.string.feat_theme), getString(R.string.feat_theme_sub), () -> go(ThemePickerActivity.class)));
        items.add(new NavItem("ℹ️", getString(R.string.feat_about), getString(R.string.feat_about_sub), () -> go(AboutActivity.class)));
        items.add(new NavItem("📉", getString(R.string.feat_unused_app), getString(R.string.feat_unused_app_sub), () -> go(UnusedAppActivity.class)));
        items.add(new NavItem("🔐", getString(R.string.feat_permission_center), getString(R.string.feat_permission_center_sub), () -> go(PermissionCenterActivity.class)));
        items.add(new NavItem("🛡️", getString(R.string.feat_root), getString(R.string.feat_root_sub), () -> go(RootStatusActivity.class)));
        items.add(new NavItem("🔒", getString(R.string.feat_privacy), getString(R.string.feat_privacy_sub), () -> PolicyActivity.open(this, PolicyActivity.MODE_PRIVACY)));
        items.add(new NavItem("📜", getString(R.string.feat_agreement), getString(R.string.feat_agreement_sub), () -> PolicyActivity.open(this, PolicyActivity.MODE_AGREEMENT)));

        NavAdapter navAdapter = new NavAdapter(items);
        rv.setAdapter(navAdapter);
        EditText etSearch = container.findViewById(R.id.et_nav_search);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(android.text.Editable s) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                    navAdapter.filter(s == null ? "" : s.toString());
                }
            });
        }
    }

    private void goClean(int type) {
        Intent ci = new Intent(this, ScanResultActivity.class);
        ci.putExtra(ScanResultActivity.EXTRA_CLEAN_TYPE, type);
        startActivity(ci);
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
        private final List<NavItem> allData;
        private final List<NavItem> data;

        NavAdapter(List<NavItem> all) {
            this.allData = new ArrayList<>(all);
            this.data = new ArrayList<>(all);
        }

        void filter(String q) {
            q = q == null ? "" : q.trim().toLowerCase();
            data.clear();
            if (q.isEmpty()) {
                data.addAll(allData);
            } else {
                boolean headerKept = false;
                for (NavItem it : allData) {
                    if (it.isHeader) { headerKept = false; continue; }
                    if ((it.title != null && it.title.toLowerCase().contains(q))
                            || (it.sub != null && it.sub.toLowerCase().contains(q))) {
                        if (!headerKept) {
                            int idx = allData.indexOf(it) - 1;
                            while (idx >= 0 && !allData.get(idx).isHeader) idx--;
                            if (idx >= 0) { data.add(allData.get(idx)); headerKept = true; }
                            else headerKept = true;
                        }
                        data.add(it);
                    }
                }
            }
            notifyDataSetChanged();
        }

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

        RecyclerView rv = container.findViewById(R.id.rv_home_grid);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        List<FeatureItem> items = new ArrayList<>();
        items.add(new FeatureItem("🧹", getString(R.string.feat_junk), "", () -> startScan(false)));
        items.add(new FeatureItem("📁", getString(R.string.feat_empty), "", () -> startScan(true)));
        items.add(new FeatureItem("🗑️", getString(R.string.clean_residual), "", () -> goClean(AppCleanScanner.TYPE_RESIDUAL)));
        items.add(new FeatureItem("💬", getString(R.string.clean_wechat), "", () -> goClean(AppCleanScanner.TYPE_WECHAT)));
        items.add(new FeatureItem("🐧", getString(R.string.clean_qq), "", () -> goClean(AppCleanScanner.TYPE_QQ)));
        items.add(new FeatureItem("🎵", getString(R.string.clean_douyin), "", () -> goClean(AppCleanScanner.TYPE_DOUYIN)));
        items.add(new FeatureItem("🌐", getString(R.string.clean_browser), "", () -> goClean(AppCleanScanner.TYPE_BROWSER)));
        items.add(new FeatureItem("📦", getString(R.string.clean_apk), "", () -> goClean(AppCleanScanner.TYPE_APK)));
        items.add(new FeatureItem("🖼️", getString(R.string.clean_screenshot), "", () -> goClean(AppCleanScanner.TYPE_SCREENSHOT)));
        items.add(new FeatureItem("📜", getString(R.string.clean_log), "", () -> goClean(AppCleanScanner.TYPE_LOG)));
        items.add(new FeatureItem("🗂️", getString(R.string.clean_temp), "", () -> goClean(AppCleanScanner.TYPE_TEMP)));
        items.add(new FeatureItem("🐘", getString(R.string.clean_large), "", () -> goClean(AppCleanScanner.TYPE_LARGE)));
        items.add(new FeatureItem("🖼", getString(R.string.clean_thumb), "", () -> goClean(AppCleanScanner.TYPE_THUMB)));
        items.add(new FeatureItem("🚫", getString(R.string.clean_ad), "", () -> goClean(AppCleanScanner.TYPE_AD)));
        items.add(new FeatureItem("📑", getString(R.string.feat_dup), "", () -> go(DuplicateActivity.class)));
        items.add(new FeatureItem("💽", getString(R.string.feat_partition), "", () -> go(PartitionActivity.class)));
        items.add(new FeatureItem("⚡", getString(R.string.feat_memboost), "", () -> go(MemoryBoostActivity.class)));
        items.add(new FeatureItem("🛠️", getString(R.string.feat_custom), "", () -> go(CustomCleanActivity.class)));
        items.add(new FeatureItem("🔐", getString(R.string.feat_permission_center), getString(R.string.feat_permission_center_sub), () -> go(PermissionCenterActivity.class)));
        items.add(new FeatureItem("📤", getString(R.string.extract_title), "", () -> go(ApkExtractActivity.class)));
        items.add(new FeatureItem("🖼️", getString(R.string.feat_photo), getString(R.string.feat_photo_sub), () -> goClean(AppCleanScanner.TYPE_PHOTO)));
        items.add(new FeatureItem("📚", getString(R.string.feat_ebook), getString(R.string.feat_ebook_sub), () -> goClean(AppCleanScanner.TYPE_EBOOK)));
        items.add(new FeatureItem("🗄️", getString(R.string.feat_app_data), getString(R.string.feat_app_data_sub), () -> goClean(AppCleanScanner.TYPE_APP_DATA)));
        items.add(new FeatureItem("📉", getString(R.string.feat_unused_app), getString(R.string.feat_unused_app_sub), () -> go(UnusedAppActivity.class)));
        items.add(new FeatureItem("🔥", getString(R.string.feat_deep), getString(R.string.feat_deep_sub), () -> goClean(AppCleanScanner.TYPE_DEEP)));
        items.add(new FeatureItem("🛡️", getString(R.string.feat_root), getString(R.string.feat_root_sub), () -> go(RootStatusActivity.class)));
        items.add(new FeatureItem("🔒", getString(R.string.feat_privacy), getString(R.string.feat_privacy_sub), () -> PolicyActivity.open(this, PolicyActivity.MODE_PRIVACY)));
        items.add(new FeatureItem("📜", getString(R.string.feat_agreement), getString(R.string.feat_agreement_sub), () -> PolicyActivity.open(this, PolicyActivity.MODE_AGREEMENT)));
        items.add(new FeatureItem("🏢", getString(R.string.feat_company), getString(R.string.feat_company_sub), () -> go(EnterpriseActivity.class)));
        rv.setAdapter(new FeatureAdapter(items, R.layout.item_home_feature));

        View statInstalled = container.findViewById(R.id.stat_installed);
        if (statInstalled != null) {
            statInstalled.setOnClickListener(v -> go(ApkExtractActivity.class));
        }

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
                    battTemp = batt.getIntExtra("temperature", 0);
                    battHealth = batt.getIntExtra("health", 0);
                    battStatus = batt.getIntExtra("status", 0);
                }
            } catch (Exception ignored) {}

            // CPU
            int cores = Runtime.getRuntime().availableProcessors();
            String freq = readCpuFreq();
            String cpuModel = safe(Build.HARDWARE) + " · " + safe(Build.BOARD);
            String model = safe(Build.BRAND) + " " + safe(Build.MODEL);
            String androidVer = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
            String screen = getScreenInfo();

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
            final String fFreq = freq, fCpuModel = cpuModel, fModel = model,
                    fAndroid = androidVer, fScreen = screen;

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
                setText(R.id.tv_battery_temp, String.format("%.1f°C", fBattTemp / 10.0));
                setText(R.id.tv_battery_health, healthText(fBattHealth));
                if (fBattStatus == BatteryManager.BATTERY_STATUS_CHARGING) {
                    setText(R.id.tv_battery_level, (fBattLevel >= 0 ? fBattLevel + "%" : "—") + " ⚡");
                }

                setText(R.id.tv_cpu_cores, String.valueOf(fCores));
                setText(R.id.tv_cpu_freq, fFreq);
                setText(R.id.tv_cpu_model, fCpuModel);

                setText(R.id.tv_sensors, String.valueOf(fSensors));
                setText(R.id.tv_apps, String.valueOf(fApps));
                setText(R.id.tv_model, fModel);
                setText(R.id.tv_android, fAndroid);
                setText(R.id.tv_screen, fScreen);
            });
        }).start();
    }

    private String getScreenInfo() {
        try {
            android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            return dm.widthPixels + "×" + dm.heightPixels + " · " + dm.densityDpi + "dpi";
        } catch (Exception e) {
            return "未知";
        }
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
        items.add(new FeatureItem("🔐", getString(R.string.feat_permission_center), getString(R.string.feat_permission_center_sub), () -> go(PermissionCenterActivity.class)));
        items.add(new FeatureItem("🛡️", getString(R.string.feat_root), getString(R.string.feat_root_sub), () -> go(RootStatusActivity.class)));
        items.add(new FeatureItem("🔒", getString(R.string.feat_privacy), getString(R.string.feat_privacy_sub), () -> PolicyActivity.open(this, PolicyActivity.MODE_PRIVACY)));
        items.add(new FeatureItem("📜", getString(R.string.feat_agreement), getString(R.string.feat_agreement_sub), () -> PolicyActivity.open(this, PolicyActivity.MODE_AGREEMENT)));
        items.add(new FeatureItem("🌐", getString(R.string.feat_website), getString(R.string.feat_website_sub), () -> openUrl(RELEASES_URL)));
        items.add(new FeatureItem("📤", getString(R.string.feat_share), getString(R.string.feat_share_sub), this::shareApp));
        items.add(new FeatureItem("📦", getString(R.string.feat_source), getString(R.string.feat_source_sub), () -> go(SourceActivity.class)));
        rv.setAdapter(new FeatureAdapter(items));

        setupProfileHeader();

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

    /* ===================== 资料页（可自定义头像/昵称/签名） ===================== */
    private void setupProfileHeader() {
        ImageView avatar = container.findViewById(R.id.iv_profile_avatar);
        TextView name = container.findViewById(R.id.tv_profile_name);
        TextView sign = container.findViewById(R.id.tv_profile_sign);
        View avatarBox = container.findViewById(R.id.cv_profile_avatar);
        TextView level = container.findViewById(R.id.tv_profile_level);

        avatar.setImageResource(ProfilePrefs.getAvatarResId(this));
        name.setText(ProfilePrefs.getName(this));
        sign.setText(ProfilePrefs.getSign(this));
        if (level != null) level.setText(R.string.user_level);

        avatarBox.setOnClickListener(v -> showAvatarPicker(avatar));
        name.setOnClickListener(v -> showNameEditor(name));
        sign.setOnClickListener(v -> showSignEditor(sign));
    }

    private void showAvatarPicker(ImageView avatar) {
        GridView gv = new GridView(this);
        gv.setNumColumns(4);
        gv.setHorizontalSpacing(16);
        gv.setVerticalSpacing(16);
        gv.setPadding(24, 24, 24, 24);
        List<Integer> resIds = new ArrayList<>();
        for (String n : ProfilePrefs.AVATARS) {
            int id = getResources().getIdentifier(n, "drawable", getPackageName());
            resIds.add(id == 0 ? R.drawable.avatar_01 : id);
        }
        gv.setAdapter(new BaseAdapter() {
            @Override public int getCount() { return resIds.size(); }
            @Override public Object getItem(int i) { return resIds.get(i); }
            @Override public long getItemId(int i) { return i; }
            @Override public View getView(int i, View v, ViewGroup p) {
                ImageView iv = new ImageView(MainActivity.this);
                iv.setLayoutParams(new ViewGroup.LayoutParams(120, 120));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setImageResource(resIds.get(i));
                return iv;
            }
        });
        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.profile_avatar_title)
                .setView(gv)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        gv.setOnItemClickListener((parent, view, pos, id) -> {
            ProfilePrefs.setAvatar(this, ProfilePrefs.AVATARS[pos]);
            avatar.setImageResource(resIds.get(pos));
            dlg.dismiss();
        });
        dlg.show();
    }

    private void showNameEditor(TextView name) {
        EditText et = new EditText(this);
        et.setHint(R.string.profile_name_hint);
        et.setText(ProfilePrefs.getName(this));
        et.setSelection(et.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_edit_name)
                .setView(et)
                .setPositiveButton(R.string.profile_save, (d, w) -> {
                    String v = et.getText().toString().trim();
                    if (v.isEmpty()) v = ProfilePrefs.DEFAULT_NAME;
                    ProfilePrefs.setName(this, v);
                    name.setText(v);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSignEditor(TextView sign) {
        EditText et = new EditText(this);
        et.setHint(R.string.profile_sign_hint);
        et.setText(ProfilePrefs.getSign(this));
        et.setSelection(et.getText().length());
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_edit_sign)
                .setView(et)
                .setPositiveButton(R.string.profile_save, (d, w) -> {
                    String v = et.getText().toString().trim();
                    if (v.isEmpty()) v = ProfilePrefs.DEFAULT_SIGN;
                    ProfilePrefs.setSign(this, v);
                    sign.setText(v);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /* ===================== 文件管理 Tab ===================== */
    private String currentFilesPath;

    private void setupFiles() {
        if (currentFilesPath == null) {
            File root = Environment.getExternalStorageDirectory();
            currentFilesPath = root != null ? root.getAbsolutePath() : "/";
        }
        RecyclerView rv = container.findViewById(R.id.rv_files);
        rv.setLayoutManager(new LinearLayoutManager(this));
        MaterialButton btnUp = container.findViewById(R.id.btn_file_up);
        FloatingActionButton fabRoot = container.findViewById(R.id.fab_file_root);

        btnUp.setOnClickListener(v -> {
            File parent = new File(currentFilesPath).getParentFile();
            if (parent != null && parent.canRead()) {
                currentFilesPath = parent.getAbsolutePath();
                refreshFiles();
            }
        });
        fabRoot.setOnClickListener(v -> {
            File root = Environment.getExternalStorageDirectory();
            currentFilesPath = root != null ? root.getAbsolutePath() : "/";
            refreshFiles();
        });
        refreshFiles();
    }

    private void refreshFiles() {
        TextView pathView = container.findViewById(R.id.tv_file_path);
        RecyclerView rv = container.findViewById(R.id.rv_files);
        pathView.setText(currentFilesPath);
        List<FileItem> list = listFiles(currentFilesPath);
        if (list.isEmpty()) {
            Toast.makeText(this, R.string.file_empty, Toast.LENGTH_SHORT).show();
        }
        rv.setAdapter(new FileAdapter(list,
                (file, isDir) -> {
                    if (isDir) {
                        currentFilesPath = file.getAbsolutePath();
                        refreshFiles();
                    } else {
                        openFile(file);
                    }
                },
                (file) -> {
                    File parent = file.getParentFile();
                    if (parent != null && parent.canRead()) {
                        currentFilesPath = parent.getAbsolutePath();
                        refreshFiles();
                        Toast.makeText(this, R.string.file_jump_dir, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, R.string.file_open_fail, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private List<FileItem> listFiles(String path) {
        List<FileItem> result = new ArrayList<>();
        File dir = new File(path);
        File[] files;
        try {
            files = dir.listFiles();
        } catch (Exception e) {
            files = null;
        }
        if (files == null) return result;
        List<File> dirs = new ArrayList<>();
        List<File> regs = new ArrayList<>();
        for (File f : files) {
            if (f.isDirectory()) dirs.add(f); else regs.add(f);
        }
        dirs.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        regs.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File f : dirs) result.add(new FileItem(f, true));
        for (File f : regs) result.add(new FileItem(f, false));
        return result;
    }

    private void openFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            String ext = MimeTypeMap.getFileExtensionFromUrl(file.getName());
            String mime = ext != null ? MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase()) : null;
            if (mime == null) mime = "*/*";
            Intent it = new Intent(Intent.ACTION_VIEW);
            it.setDataAndType(uri, mime);
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(it);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.file_open_fail, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, R.string.file_open_fail, Toast.LENGTH_SHORT).show();
        }
    }

    static class FileItem {
        final File file;
        final boolean isDir;
        FileItem(File f, boolean d) { this.file = f; this.isDir = d; }
    }

    interface OnFileClick { void onClick(File f, boolean isDir); }
    interface OnFileLong { void onLong(File f); }

    class FileAdapter extends RecyclerView.Adapter<FileAdapter.VH> {
        private final List<FileItem> data;
        private final OnFileClick click;
        private final OnFileLong longClick;
        FileAdapter(List<FileItem> d, OnFileClick c, OnFileLong l) { data = d; click = c; longClick = l; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            View v = LayoutInflater.from(p.getContext()).inflate(R.layout.item_file, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            FileItem it = data.get(pos);
            String name = it.file.getName();
            h.name.setText(name.isEmpty() ? "/" : name);
            h.icon.setText(it.isDir ? "📁" : "📄");
            h.sub.setText(it.isDir ? "文件夹" : Util.formatSize(it.file.length()));
            h.itemView.setOnClickListener(v -> click.onClick(it.file, it.isDir));
            h.itemView.setOnLongClickListener(v -> { longClick.onLong(it.file); return true; });
        }

        @Override
        public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView icon, name, sub;
            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.tv_file_icon);
                name = v.findViewById(R.id.tv_file_name);
                sub = v.findViewById(R.id.tv_file_sub);
            }
        }
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
        private final int layoutRes;
        FeatureAdapter(List<FeatureItem> data) { this(data, R.layout.item_feature); }
        FeatureAdapter(List<FeatureItem> data, int layoutRes) { this.data = data; this.layoutRes = layoutRes; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
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
