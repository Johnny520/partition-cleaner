package com.example.partitioncleaner;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 独立的 Root 状态页：实时展示设备是否已获取 root、是否安装了 root 管理器，
 * 并支持点击按钮请求授权。进入页面默认只做静默检测（不弹窗），点击按钮才弹窗申请。
 */
public class RootStatusActivity extends BaseActivity {

    private TextView tvStatus;
    private TextView tvDetail;
    private Button btnRequest;

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
        btnRequest = findViewById(R.id.btn_request_root);
        btnRequest.setOnClickListener(v -> refresh(false));

        refresh(true);
    }

    @SuppressWarnings("deprecation")
    private void refresh(boolean silent) {
        boolean binary = RootShell.suBinaryExists();
        boolean granted = false;
        if (!silent) {
            RootShell shell = new RootShell();
            granted = shell.requestRoot();
            shell.close();
        }

        int color;
        String status;
        if (granted) {
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
        tvDetail.setText("su 二进制存在：" + binary + "\nroot shell 可用：" + granted);
    }
}
