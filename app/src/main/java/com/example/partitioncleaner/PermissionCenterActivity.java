package com.example.partitioncleaner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * 独立的“权限中心”：集中展示并管理本应用所需的各项授权
 * （存储 / 通知 / Shizuku 免 Root / Root），与 Root 状态页相互独立。
 */
public class PermissionCenterActivity extends BaseActivity {

    private static final int REQ_STORAGE = 2001;
    private static final int REQ_NOTIFY = 2002;

    private LinearLayout ll;

    private static class Perm {
        final String title;
        final String emoji;
        final int kind;

        Perm(String title, String emoji, int kind) {
            this.title = title;
            this.emoji = emoji;
            this.kind = kind;
        }
    }

    private static final int KIND_STORAGE = 0;
    private static final int KIND_NOTIFY = 1;
    private static final int KIND_SHIZUKU = 2;
    private static final int KIND_ROOT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_center);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ll = findViewById(R.id.ll_perms);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        build(); // 返回时刷新状态
    }

    private void build() {
        ll.removeAllViews();
        List<Perm> list = new ArrayList<>();
        list.add(new Perm(getString(R.string.perm_storage), "📁", KIND_STORAGE));
        list.add(new Perm(getString(R.string.perm_notification), "🔔", KIND_NOTIFY));
        list.add(new Perm(getString(R.string.perm_shizuku), "🛡️", KIND_SHIZUKU));
        list.add(new Perm(getString(R.string.perm_root), "⚡", KIND_ROOT));

        for (Perm p : list) {
            ll.addView(makeRow(p));
        }
    }

    private View makeRow(Perm p) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (getResources().getDisplayMetrics().density * 12), 0, 0);

        TextView icon = new TextView(this);
        icon.setText(p.emoji);
        icon.setTextSize(28);
        icon.setPadding(0, 0, (int) (getResources().getDisplayMetrics().density * 12), 0);
        row.addView(icon);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = new TextView(this);
        title.setText(p.title);
        title.setTextSize(16);
        title.setTextColor(ContextCompat.getColor(this, R.color.onSurface));

        TextView status = new TextView(this);
        status.setTextSize(13);
        boolean ok = isGranted(p.kind);
        status.setText((ok ? "● " : "○ ") + (ok ? getString(R.string.perm_granted) : getString(R.string.perm_denied)));
        status.setTextColor(ContextCompat.getColor(this, ok ? R.color.advice_clean : R.color.advice_keep));

        texts.addView(title);
        texts.addView(status);
        row.addView(texts);

        Button btn = new Button(this);
        btn.setText(ok ? getString(R.string.perm_open) : getString(R.string.perm_grant));
        btn.setTextSize(13);
        btn.setOnClickListener(v -> onAction(p.kind));
        row.addView(btn);

        return row;
    }

    private boolean isGranted(int kind) {
        switch (kind) {
            case KIND_STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
                }
                return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            case KIND_NOTIFY:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true;
                return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
            case KIND_SHIZUKU:
                return ShizukuAccess.isShizukuAvailable(this);
            case KIND_ROOT:
                return RootShell.suBinaryExists();
            default:
                return false;
        }
    }

    private void onAction(int kind) {
        switch (kind) {
            case KIND_STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO}, REQ_STORAGE);
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQ_STORAGE);
                }
                break;
            case KIND_NOTIFY:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
                } else {
                    Toast.makeText(this, R.string.perm_not_needed, Toast.LENGTH_SHORT).show();
                }
                break;
            case KIND_SHIZUKU:
                ShizukuAccess.requestShizuku(this);
                Toast.makeText(this, R.string.shizuku_guide_toast, Toast.LENGTH_LONG).show();
                break;
            case KIND_ROOT:
                startActivity(new Intent(this, RootStatusActivity.class));
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        build();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
