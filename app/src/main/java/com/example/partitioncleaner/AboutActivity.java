/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends BaseActivity {

    private static final String REPO_URL = "https://github.com/Johnny520/partition-cleaner";
    private static final String RELEASES_URL = "https://github.com/Johnny520/partition-cleaner/releases";
    private static final String HOME_URL = "https://github.com/Johnny520";
    private static final String SITE_URL = "https://johnny520.github.io/Johnny/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.about_title);

        TextView tvVersion = findViewById(R.id.tv_version);
        if (tvVersion != null) {
            tvVersion.setText("版本 v" + BuildConfig.VERSION_NAME);
        }

        Button btn = findViewById(R.id.btn_open_repo);
        btn.setOnClickListener(v -> openUrl(RELEASES_URL));
        Button btnHome = findViewById(R.id.btn_home);
        if (btnHome != null) btnHome.setOnClickListener(v -> openUrl(HOME_URL));
        Button btnSite = findViewById(R.id.btn_site);
        if (btnSite != null) btnSite.setOnClickListener(v -> openUrl(SITE_URL));
        Button btnExport = findViewById(R.id.btn_export_log);
        if (btnExport != null) btnExport.setOnClickListener(v -> exportCrashLog());
    }

    private void exportCrashLog() {
        java.io.File log = CrashHandler.getLatestCrashLog(this);
        if (log == null || !log.exists()) {
            Toast.makeText(this, "暂无崩溃日志", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(log))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append("\n");
            }
            android.content.ClipboardManager cm = getSystemService(android.content.ClipboardManager.class);
            if (cm != null) {
                cm.setPrimaryClip(android.content.ClipData.newPlainText("crash_log", sb.toString()));
                Toast.makeText(this, "崩溃日志已复制，粘贴发给开发者即可", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "无法访问剪贴板", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "读取日志失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开浏览器：" + url, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
