package com.example.partitioncleaner;

import android.content.pm.ApplicationInfo;
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

    static class AppEntry {
        String label;
        String pkg;
        String src;
        long size;
        Drawable icon;
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
                try {
                    e.icon = ai.loadIcon(pm);
                } catch (Exception ignored) {
                }
                list.add(e);
            }
            Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
            final List<AppEntry> sorted = list;
            runOnUiThread(() -> adapter.setItems(sorted));
        }).start();
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
            h.btn.setOnClickListener(v -> extract(e));
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
