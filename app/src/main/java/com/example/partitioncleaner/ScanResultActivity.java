package com.example.partitioncleaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.ChipGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ScanResultActivity extends BaseActivity {

    public static final String EXTRA_EMPTY_ONLY = "empty_only";

    private static final int FILTER_ALL = 0;
    private static final int FILTER_CLEAN = 1;
    private static final int FILTER_KEEP = 2;

    private RecyclerView rv;
    private JunkAdapter adapter;
    private ProgressBar pb;
    private TextView tvSummary;
    private Button btnClean;
    private Button btnCleanRec;
    private ChipGroup chipFilter;

    private List<JunkItem> items = new ArrayList<>();        // 全量扫描结果
    private List<JunkItem> displayItems = new ArrayList<>(); // 当前筛选后显示
    private RootShell rootShell;
    private boolean emptyOnly;
    private int filterMode = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        emptyOnly = getIntent().getBooleanExtra(EXTRA_EMPTY_ONLY, false);
        setTitle(emptyOnly ? "空文件夹查询" : "垃圾扫描结果");

        rv = findViewById(R.id.rv_junk);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JunkAdapter(displayItems);
        rv.setAdapter(adapter);

        pb = findViewById(R.id.pb);
        tvSummary = findViewById(R.id.tv_summary);
        btnClean = findViewById(R.id.btn_clean);
        btnCleanRec = findViewById(R.id.btn_clean_recommended);
        chipFilter = findViewById(R.id.chip_filter);

        btnClean.setOnClickListener(v -> doClean(true));
        btnCleanRec.setOnClickListener(v -> doClean(false));

        chipFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_clean) filterMode = FILTER_CLEAN;
            else if (checkedId == R.id.chip_keep) filterMode = FILTER_KEEP;
            else filterMode = FILTER_ALL;
            applyFilter();
        });

        startScan();
    }

    private void startScan() {
        pb.setVisibility(View.VISIBLE);
        btnClean.setEnabled(false);
        btnCleanRec.setEnabled(false);
        new Thread(() -> {
            rootShell = new RootShell();
            boolean root = rootShell.requestRoot();
            final List<JunkItem> result = new ArrayList<>();
            if (emptyOnly) {
                String[] roots = {"/data", "/storage/emulated/0", "/cache", "/system", "/cust"};
                result.addAll(JunkScanner.scanEmptyDirs(rootShell, roots));
            } else {
                result.addAll(JunkScanner.scanUserJunk());
                if (root) result.addAll(JunkScanner.scanRootJunk(rootShell));
            }
            items.clear();
            items.addAll(result);
            runOnUiThread(() -> {
                applyFilter();
                pb.setVisibility(View.GONE);
                long cleanSize = 0;
                int cleanCnt = 0, keepCnt = 0;
                for (JunkItem it : items) {
                    if (it.advice == JunkItem.ADVICE_KEEP) keepCnt++;
                    else {
                        cleanCnt++;
                        cleanSize += it.size;
                    }
                }
                tvSummary.setText("共 " + items.size() + " 项 ｜ 建议清理 " + cleanCnt
                        + " 项（可释放 " + Util.formatSize(cleanSize) + "）｜ 建议保留 " + keepCnt + " 项");
                btnClean.setEnabled(!items.isEmpty());
                btnCleanRec.setEnabled(cleanCnt > 0);
                if (items.isEmpty()) {
                    Toast.makeText(this, "未发现可清理项", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /** 根据 filterMode 刷新显示的列表。 */
    private void applyFilter() {
        displayItems.clear();
        for (JunkItem it : items) {
            if (filterMode == FILTER_CLEAN && it.advice != JunkItem.ADVICE_CLEAN) continue;
            if (filterMode == FILTER_KEEP && it.advice != JunkItem.ADVICE_KEEP) continue;
            displayItems.add(it);
        }
        adapter.notifyDataSetChanged();
    }

    /** 清理：useSelected=true 清勾选项；false 直接清所有“建议清理”项。 */
    private void doClean(boolean useSelected) {
        if (rootShell == null || !rootShell.isAvailable()) {
            Toast.makeText(this, "清理系统分区需要 Root 权限", Toast.LENGTH_LONG).show();
            return;
        }
        pb.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<JunkItem> toClean = new ArrayList<>();
            String msg;
            if (useSelected) {
                for (JunkItem it : items) if (it.selected) toClean.add(it);
                msg = Cleaner.clean(rootShell, toClean);
            } else {
                for (JunkItem it : items) if (it.advice == JunkItem.ADVICE_CLEAN) toClean.add(it);
                msg = Cleaner.cleanForce(rootShell, toClean);
            }
            final String result = msg;
            runOnUiThread(() -> {
                pb.setVisibility(View.GONE);
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                startScan(); // 清理后刷新
            });
        }).start();
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
}
