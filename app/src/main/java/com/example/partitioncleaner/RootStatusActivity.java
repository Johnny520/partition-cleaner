/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 独立的 Root 状态页：实时展示设备是否已获取 Root、是否安装了 Root 管理器，
 * 以及 Shizuku 免 Root 授权状态。进入页面只做静默检测（不弹窗），每 3 秒自动刷新；
 * 点击按钮才主动请求 Root / Shizuku 授权。
 */
public class RootStatusActivity extends BaseActivity {

    private TextView tvStatus;
    private TextView tvDetail;
    private TextView tvShizuku;
    private TextView tvShizukuDetail;
    private Button btnRequest;
    private Button btnShizuku;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean paused = false;
    private boolean rootGranted = false;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!paused) {
                refresh(true);
                handler.postDelayed(this, 3000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_root_status);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvStatus = findViewById(R.id.tv_root_status);
        tvDetail = findViewById(R.id.tv_root_detail);
        tvShizuku = findViewById(R.id.tv_shizuku_status);
        tvShizukuDetail = findViewById(R.id.tv_shizuku_detail);
        btnRequest = findViewById(R.id.btn_request_root);
        btnShizuku = findViewById(R.id.btn_request_shizuku);

        btnRequest.setOnClickListener(v -> refresh(false));
        btnShizuku.setOnClickListener(v -> {
            if (ShizukuAccess.isBinderAvailable()) {
                // 管理器已运行：直接应用内弹窗授权
                ShizukuAccess.requestPermission(granted -> {
                    Toast.makeText(this, granted ? R.string.shizuku_granted : R.string.shizuku_denied, Toast.LENGTH_SHORT).show();
                    refresh(false);
                });
            } else {
                // 管理器未运行：引导用户打开 Shizuku 启动/授权
                ShizukuAccess.requestShizuku(this);
                Toast.makeText(this, R.string.shizuku_guide_toast, Toast.LENGTH_LONG).show();
                refresh(false);
            }
        });

        refresh(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        handler.postDelayed(tick, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
        handler.removeCallbacks(tick);
    }

    @SuppressWarnings("deprecation")
    private void refresh(boolean silent) {
        boolean binary = RootShell.suBinaryExists();
        if (!silent) {
            RootShell shell = new RootShell();
            rootGranted = shell.requestRoot();
            shell.close();
        }

        int color;
        String status;
        if (rootGranted) {
            status = "已获取 Root 权限";
            color = getResources().getColor(android.R.color.holo_green_dark);
        } else if (binary) {
            status = "未授权（检测到 Root 管理器）";
            color = getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            status = "未获取 Root 权限";
            color = getResources().getColor(android.R.color.holo_red_dark);
        }
        tvStatus.setText(status);
        tvStatus.setTextColor(color);
        tvDetail.setText("su 二进制存在：" + binary + "\nroot shell 可用：" + rootGranted);

        boolean installed = ShizukuAccess.isManagerInstalled(this);
        boolean shizukuOk = ShizukuAccess.isShizukuAvailable(this);
        int sc;
        String sstatus;
        if (shizukuOk) {
            sstatus = "已授权（免 Root 可用）";
            sc = getResources().getColor(android.R.color.holo_green_dark);
        } else if (installed) {
            sstatus = "已安装，待授权";
            sc = getResources().getColor(android.R.color.holo_orange_dark);
        } else {
            sstatus = "未安装 Shizuku";
            sc = getResources().getColor(android.R.color.holo_red_dark);
        }
        tvShizuku.setText(sstatus);
        tvShizuku.setTextColor(sc);
        tvShizukuDetail.setText("管理器已装：" + installed
                + "\nAPI_V23 授权：" + ShizukuAccess.isPermissionGranted(this));
    }
}
