package com.example.partitioncleaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvPartitions;
    private PartitionAdapter adapter;
    private CircularProgressIndicator pb;
    private TextView tvRoot;
    private List<PartitionInfo> partitions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setSupportActionBar(findViewById(R.id.toolbar));

        rvPartitions = findViewById(R.id.rv_partitions);
        rvPartitions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PartitionAdapter(partitions);
        rvPartitions.setAdapter(adapter);

        pb = findViewById(R.id.pb_root);
        tvRoot = findViewById(R.id.tv_root);
        MaterialCardView cardRoot = findViewById(R.id.card_root);
        cardRoot.setOnClickListener(v -> requestRootStatus());
        MaterialCardView cardToolbox = findViewById(R.id.card_toolbox);
        cardToolbox.setOnClickListener(v -> startActivity(new Intent(this, ToolboxActivity.class)));

        Button btnScan = findViewById(R.id.btn_scan);
        Button btnEmpty = findViewById(R.id.btn_empty);
        Button btnDup = findViewById(R.id.btn_dup);
        btnScan.setOnClickListener(v -> openScan(false));
        btnEmpty.setOnClickListener(v -> openScan(true));
        btnDup.setOnClickListener(v -> startActivity(new Intent(this, DuplicateActivity.class)));

        checkRootAndScan();
    }

    private void checkRootAndScan() {
        requestRootStatus();
    }

    private void requestRootStatus() {
        pb.setVisibility(View.VISIBLE);
        new Thread(() -> {
            boolean suThere = RootShell.suBinaryExists();
            RootShell root = new RootShell();
            boolean ok = root.requestRoot();
            root.close();
            boolean finalOk = ok;
            boolean finalSu = suThere;
            runOnUiThread(() -> {
                if (finalOk) {
                    tvRoot.setText("Root 已授权");
                } else if (finalSu) {
                    tvRoot.setText("检测到 Root 管理器，但授权被拒绝——可在 Magisk/KernelSU 中允许本应用后点此重试");
                } else {
                    tvRoot.setText("未检测到 Root 管理器（需安装 Magisk/KernelSU），仅能扫描用户区文件，点此可重试");
                }
                loadPartitions();
            });
        }).start();
    }

    private void loadPartitions() {
        new Thread(() -> {
            List<PartitionInfo> list = new ArrayList<>();
            list.addAll(PartitionScanner.scanFilesystem());
            list.addAll(PartitionScanner.scanMemory());
            partitions.clear();
            partitions.addAll(list);
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                pb.setVisibility(View.GONE);
            });
        }).start();
    }

    private void openScan(boolean emptyOnly) {
        Intent intent = new Intent(this, ScanResultActivity.class);
        intent.putExtra(ScanResultActivity.EXTRA_EMPTY_ONLY, emptyOnly);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
