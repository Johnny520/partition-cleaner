package com.example.partitioncleaner;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ScanResultActivity extends BaseActivity {

    public static final String EXTRA_EMPTY_ONLY = "empty_only";
    public static final String EXTRA_CLEAN_TYPE = "clean_type";
    public static final String EXTRA_CUSTOM_RULE_ID = "custom_rule_id";

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
    private Button btnSelectAll;
    private Button btnDeselectAll;
    private TextView tvSelectedSize;

    private List<JunkItem> items = new ArrayList<>();        // 全量扫描结果
    private List<JunkItem> displayItems = new ArrayList<>(); // 当前筛选后显示
    private RootShell rootShell;
    private boolean emptyOnly;
    private int cleanType = -1;
    private int filterMode = FILTER_ALL;
    private CustomRule customRule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        emptyOnly = getIntent().getBooleanExtra(EXTRA_EMPTY_ONLY, false);
        cleanType = getIntent().getIntExtra(EXTRA_CLEAN_TYPE, -1);
        String ruleId = getIntent().getStringExtra(EXTRA_CUSTOM_RULE_ID);
        if (ruleId != null) customRule = new CustomRuleStore(this).getById(ruleId);
        if (customRule != null) setTitle(customRule.name);
        else if (cleanType > 0) setTitle(getString(AppCleanScanner.getTitleRes(cleanType)));
        else setTitle(emptyOnly ? "空文件夹查询" : "垃圾扫描结果");

        rv = findViewById(R.id.rv_junk);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new JunkAdapter(displayItems);
        adapter.setOnItemClick(this::showJunkDetail);
        adapter.setOnSelectionChanged(this::updateSelectedSize);
        rv.setAdapter(adapter);

        pb = findViewById(R.id.pb);
        tvSummary = findViewById(R.id.tv_summary);
        btnClean = findViewById(R.id.btn_clean);
        btnCleanRec = findViewById(R.id.btn_clean_recommended);
        chipFilter = findViewById(R.id.chip_filter);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnDeselectAll = findViewById(R.id.btn_deselect_all);
        tvSelectedSize = findViewById(R.id.tv_selected_size);

        btnClean.setOnClickListener(v -> doClean(true));
        btnCleanRec.setOnClickListener(v -> doClean(false));
        btnSelectAll.setOnClickListener(v -> setAllSelected(true));
        btnDeselectAll.setOnClickListener(v -> setAllSelected(false));

        chipFilter.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip_clean) filterMode = FILTER_CLEAN;
            else if (checkedId == R.id.chip_keep) filterMode = FILTER_KEEP;
            else filterMode = FILTER_ALL;
            applyFilter();
        });

        if (ensureStoragePermission()) startScan();
    }

    private static final int REQ_STORAGE = 1001;

    /** 确保存储权限；无权限则申请，无论授权与否最终都会扫描（API29+ 的 MediaStore 不强制权限即可查询）。 */
    private boolean ensureStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean ok = checkSelfPermission("android.permission.READ_MEDIA_IMAGES") == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission("android.permission.READ_MEDIA_VIDEO") == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission("android.permission.READ_MEDIA_AUDIO") == PackageManager.PERMISSION_GRANTED;
            if (!ok) {
                requestPermissions(new String[]{"android.permission.READ_MEDIA_IMAGES",
                        "android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO"}, REQ_STORAGE);
                return false;
            }
            return true;
        }
        if (checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, REQ_STORAGE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_STORAGE) startScan();
    }

    private void startScan() {
        pb.setVisibility(View.VISIBLE);
        btnClean.setEnabled(false);
        btnCleanRec.setEnabled(false);
        new Thread(() -> {
            rootShell = new RootShell();
            boolean root = rootShell.requestRoot();
            final List<JunkItem> result = new ArrayList<>();
            if (customRule != null) {
                result.addAll(AppCleanScanner.scanCustom(this, rootShell, customRule));
            } else if (cleanType > 0) {
                result.addAll(AppCleanScanner.scan(this, cleanType, rootShell));
            } else if (emptyOnly) {
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
                updateSelectedSize();
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
        updateSelectedSize();
    }

    /** 批量设置当前显示列表的勾选状态。 */
    private void setAllSelected(boolean sel) {
        for (JunkItem it : displayItems) it.selected = sel;
        adapter.notifyDataSetChanged();
        updateSelectedSize();
    }

    /** 实时统计当前显示列表中被勾选项的总大小并更新文本。 */
    private void updateSelectedSize() {
        long sel = 0;
        int cnt = 0;
        for (JunkItem it : displayItems) {
            if (it.selected) {
                sel += it.size;
                cnt++;
            }
        }
        tvSelectedSize.setText(getString(R.string.selected_size, Util.formatSize(sel), cnt));
    }

    /** 清理：useSelected=true 清勾选项；false 直接清所有“建议清理”项。 */
    private void doClean(boolean useSelected) {
        IShell shell = (rootShell != null && rootShell.isAvailable())
                ? rootShell : ShizukuAccess.getShell(this);
        if (shell == null) {
            Toast.makeText(this, "清理系统分区需要 Root 或 Shizuku 免 Root 授权", Toast.LENGTH_LONG).show();
            return;
        }
        final IShell usedShell = shell;
        pb.setVisibility(View.VISIBLE);
        new Thread(() -> {
            List<JunkItem> toClean = new ArrayList<>();
            String msg;
            if (useSelected) {
                for (JunkItem it : items) if (it.selected) toClean.add(it);
                msg = Cleaner.clean(usedShell, toClean);
            } else {
                for (JunkItem it : items) if (it.advice == JunkItem.ADVICE_CLEAN) toClean.add(it);
                msg = Cleaner.cleanForce(usedShell, toClean);
            }
            final String result = msg;
            runOnUiThread(() -> {
                pb.setVisibility(View.GONE);
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                startScan(); // 清理后刷新
            });
        }).start();
    }

    /** 点击扫描结果项时弹窗展示详情（类型/大小/建议/路径/文件状态）。 */
    private void showJunkDetail(JunkItem it) {
        StringBuilder sb = new StringBuilder();
        sb.append("类型：").append(it.typeLabel()).append("\n");
        sb.append("大小：").append(Util.formatSize(it.size)).append("\n");
        sb.append("建议：").append(it.adviceLabel()).append("\n");
        if (it.reason != null && !it.reason.isEmpty()) {
            sb.append("原因：").append(it.reason).append("\n");
        }
        sb.append("路径：").append(it.path).append("\n");
        File f = new File(it.path);
        sb.append("文件状态：").append(f.exists()
                ? "存在（实际 " + Util.formatSize(f.length()) + "）"
                : "不存在（已清理或路径失效）");
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.scan_item_detail)
                .setMessage(sb.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
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
