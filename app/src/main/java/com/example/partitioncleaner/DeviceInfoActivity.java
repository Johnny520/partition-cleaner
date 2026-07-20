package com.example.partitioncleaner;

import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class DeviceInfoActivity extends AppCompatActivity {

    private LinearLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("设备信息");

        container = findViewById(R.id.container);
        loadInfo();
    }

    private void loadInfo() {
        addRow("品牌", safe(Build.BRAND));
        addRow("型号", safe(Build.MODEL));
        addRow("厂商", safe(Build.MANUFACTURER));
        addRow("设备", safe(Build.DEVICE));
        addRow("主板", safe(Build.BOARD));
        addRow("Android 版本", safe(Build.VERSION.RELEASE));
        addRow("SDK", String.valueOf(Build.VERSION.SDK_INT));
        addRow("安全补丁", safe(Build.VERSION.SECURITY_PATCH));
        addRow("CPU ABI", Arrays.toString(Build.SUPPORTED_ABIS));

        DisplayMetrics dm = getResources().getDisplayMetrics();
        addRow("屏幕分辨率", dm.widthPixels + " × " + dm.heightPixels);
        addRow("屏幕密度", dm.densityDpi + " dpi");

        addRow("运行内存", readMemTotal());
        addRow("存储空间(总)", readStorage(false));
        addRow("存储空间(可用)", readStorage(true));
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "未知" : s;
    }

    private String readMemTotal() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        long kb = Long.parseLong(parts[1]);
                        return Util.formatSize(kb * 1024L);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            // ignore, return unknown
        }
        return "未知";
    }

    private String readStorage(boolean available) {
        try {
            StatFs stat = new StatFs(getFilesDir().getAbsolutePath());
            long bytes;
            if (Build.VERSION.SDK_INT >= 18) {
                bytes = available ? stat.getAvailableBytes() : stat.getTotalBytes();
            } else {
                long blockSize = stat.getBlockSize();
                bytes = available
                        ? (long) stat.getAvailableBlocks() * blockSize
                        : (long) stat.getBlockCount() * blockSize;
            }
            return Util.formatSize(bytes);
        } catch (Exception e) {
            return "未知";
        }
    }

    private void addRow(String label, String value) {
        if (container == null) {
            return;
        }
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, dp(10), 0, dp(10));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(14);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.onSurfaceVariant));
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(14);
        tvValue.setTextColor(ContextCompat.getColor(this, R.color.primary));
        tvValue.setGravity(Gravity.END);
        tvValue.setPadding(dp(8), 0, 0, 0);
        tvValue.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        row.addView(tvLabel);
        row.addView(tvValue);
        container.addView(row);

        View div = new View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));
        div.setBackgroundColor(ContextCompat.getColor(this, R.color.outlineVariant));
        container.addView(div);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
