package com.example.partitioncleaner;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 安装包提取：列出已安装应用，将其 APK 复制到本地目录，便于备份/分享。
 */
public class ApkExtractActivity extends BaseActivity {

    private RecyclerView rv;
    private ApkAdapter adapter;
    private RootShell rootShell;
    private File outDir;
    private com.google.android.material.chip.ChipGroup chipFilter;

    private static final int FILTER_ALL = 0;
    private static final int FILTER_USER = 1;
    private static final int FILTER_SYSTEM = 2;
    private int filterMode = FILTER_ALL;

    private List<AppEntry> allApps = new ArrayList<>();
    private List<AppEntry> filteredApps = new ArrayList<>();

    static class AppEntry {
        String label;
        String pkg;
        String src;
        long size;
        String version;
        long firstInstall;
        long lastUpdate;
        Drawable icon;
        boolean isSystem;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apk_extract);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.extract_title);

        rootShell = new RootShell();
        rootShell.requestRoot();

        // 准备输出目录（公共目录优先，失败回退到应用私有目录）
        outDir = new File(Environment.getExternalStorageDirectory(), "PartitionCleaner/APKs");
        if (!ensureDir(outDir)) {
            outDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "APKs");
            ensureDir(outDir);
        }

        rv = findViewById(R.id.rv_apk);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApkAdapter();
        rv.setAdapter(adapter);

        chipFilter = findViewById(R.id.chip_filter);
        chipFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_user) filterMode = FILTER_USER;
            else if (checkedId == R.id.chip_system) filterMode = FILTER_SYSTEM;
            else filterMode = FILTER_ALL;
            applyFilter();
        });

        com.google.android.material.floatingactionbutton.FloatingActionButton fabTop =
                findViewById(R.id.fab_top);
        com.google.android.material.floatingactionbutton.FloatingActionButton fabBottom =
                findViewById(R.id.fab_bottom);
        fabTop.setOnClickListener(v -> rv.scrollToPosition(0));
        fabBottom.setOnClickListener(v -> {
            if (adapter.getItemCount() > 0) rv.scrollToPosition(adapter.getItemCount() - 1);
        });

        loadApps();
    }

    private boolean ensureDir(File dir) {
        if (dir.exists()) return true;
        if (dir.mkdirs()) return true;
        if (rootShell != null && rootShell.isAvailable()) {
            rootShell.run("mkdir -p '" + dir.getAbsolutePath() + "' 2>/dev/null");
            return new File(dir, "").exists() || new File(dir.getAbsolutePath()).exists();
        }
        return false;
    }

    private void loadApps() {
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            List<AppEntry> list = new ArrayList<>();
            for (ApplicationInfo ai : apps) {
                AppEntry e = new AppEntry();
                e.label = ai.loadLabel(pm).toString();
                e.pkg = ai.packageName;
                e.src = ai.publicSourceDir != null ? ai.publicSourceDir : ai.sourceDir;
                e.size = (e.src != null) ? new File(e.src).length() : 0;
                e.isSystem = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                try {
                    PackageInfo pi = pm.getPackageInfo(ai.packageName, 0);
                    e.version = pi.versionName;
                    e.firstInstall = pi.firstInstallTime;
                    e.lastUpdate = pi.lastUpdateTime;
                } catch (Exception ignored) {
                }
                try {
                    e.icon = ai.loadIcon(pm);
                } catch (Exception ignored) {
                }
                list.add(e);
            }
            Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
            allApps = list;
            runOnUiThread(() -> {
                applyFilter();
                Toast.makeText(ApkExtractActivity.this,
                        getString(R.string.apk_loaded, allApps.size()), Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void applyFilter() {
        filteredApps.clear();
        for (AppEntry e : allApps) {
            if (filterMode == FILTER_USER && e.isSystem) continue;
            if (filterMode == FILTER_SYSTEM && !e.isSystem) continue;
            filteredApps.add(e);
        }
        adapter.setItems(filteredApps);
    }

    private void extract(AppEntry e) {
        if (e.src == null) {
            Toast.makeText(this, "无法读取源路径", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            String safe = (e.label + "_" + e.pkg).replaceAll("[\\\\/:*?\"<>|]", "_");
            File dest = new File(outDir, safe + ".apk");
            boolean ok = copyFile(e.src, dest.getAbsolutePath());
            final String msg = ok
                    ? "已提取：" + dest.getAbsolutePath()
                    : "提取失败，请确认存储权限或在 Root 环境下重试";
            runOnUiThread(() -> Toast.makeText(ApkExtractActivity.this, msg, Toast.LENGTH_LONG).show());
        }).start();
    }

    private boolean copyFile(String src, String destPath) {
        try {
            File f = new File(src);
            InputStream in = new FileInputStream(f);
            OutputStream out = new FileOutputStream(destPath);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            in.close();
            out.close();
            return new File(destPath).length() == f.length();
        } catch (IOException ex) {
            // 兜底：用 root 复制
            if (rootShell != null && rootShell.isAvailable()) {
                String r = rootShell.run("cp '" + src + "' '" + destPath + "' 2>/dev/null && echo OK || echo FAIL");
                return r.contains("OK");
            }
            return false;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rootShell != null) rootShell.close();
    }

    class ApkAdapter extends RecyclerView.Adapter<ApkAdapter.VH> {
        private List<AppEntry> items = new ArrayList<>();

        void setItems(List<AppEntry> l) {
            items = l;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_apk, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            AppEntry e = items.get(pos);
            if (e.icon != null) h.icon.setImageDrawable(e.icon);
            h.name.setText(e.label);
            h.pkg.setText(e.pkg);
            h.size.setText(Util.formatSize(e.size));
            h.itemView.setOnClickListener(v -> showDetail(e));
            h.btn.setOnClickListener(v -> extract(e));
        }

        private void showDetail(AppEntry e) {
            StringBuilder sb = new StringBuilder();
            sb.append("应用：").append(e.label).append("\n");
            sb.append("包名：").append(e.pkg).append("\n");
            sb.append("版本：").append(e.version == null ? "未知" : e.version).append("\n");
            sb.append("大小：").append(Util.formatSize(e.size)).append("\n");
            sb.append("安装路径：").append(e.src).append("\n");
            sb.append("首次安装：").append(formatTime(e.firstInstall)).append("\n");
            sb.append("最近更新：").append(formatTime(e.lastUpdate)).append("\n");
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(ApkExtractActivity.this)
                    .setTitle(R.string.apk_detail_title)
                    .setMessage(sb.toString())
                    .setNeutralButton(R.string.apk_copy_pkg, (d, w) -> copyToClipboard(e.pkg))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }

        private String formatTime(long t) {
            if (t <= 0) return "未知";
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(t));
            } catch (Exception e) {
                return String.valueOf(t);
            }
        }

        private void copyToClipboard(String text) {
            try {
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("pkg", text));
                Toast.makeText(ApkExtractActivity.this, R.string.apk_copied, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView name, pkg, size;
            Button btn;

            VH(View v) {
                super(v);
                icon = v.findViewById(R.id.iv_icon);
                name = v.findViewById(R.id.tv_app_name);
                pkg = v.findViewById(R.id.tv_app_pkg);
                size = v.findViewById(R.id.tv_app_size);
                btn = v.findViewById(R.id.btn_extract);
            }
        }
    }
}
