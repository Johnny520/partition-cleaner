package com.example.partitioncleaner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DuplicateActivity extends AppCompatActivity {

    private RecyclerView rv;
    private DupGroupAdapter adapter;
    private ProgressBar pb;
    private TextView tvSummary;
    private Button btnClean;
    private RootShell rootShell;
    private List<DuplicateScanner.DupGroup> groups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duplicate);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle(R.string.dup_title);

        rv = findViewById(R.id.rv_dup);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DupGroupAdapter(groups);
        rv.setAdapter(adapter);

        pb = findViewById(R.id.pb);
        tvSummary = findViewById(R.id.tv_summary);
        btnClean = findViewById(R.id.btn_clean_dup);
        btnClean.setOnClickListener(v -> doClean());

        startScan();
    }

    private void startScan() {
        pb.setVisibility(View.VISIBLE);
        btnClean.setEnabled(false);
        new Thread(() -> {
            final List<DuplicateScanner.DupGroup> result = DuplicateScanner.scan();
            groups.clear();
            groups.addAll(result);
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                pb.setVisibility(View.GONE);
                long saveable = 0;
                for (DuplicateScanner.DupGroup g : groups) {
                    saveable += g.size * (g.paths.size() - 1);
                }
                tvSummary.setText("共 " + groups.size() + " 组重复，可释放 " + Util.formatSize(saveable));
                btnClean.setEnabled(!groups.isEmpty());
                if (groups.isEmpty()) {
                    Toast.makeText(this, R.string.dup_none, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void doClean() {
        pb.setVisibility(View.VISIBLE);
        new Thread(() -> {
            rootShell = new RootShell();
            boolean root = rootShell.requestRoot();
            int ok = 0;
            int fail = 0;
            for (DuplicateScanner.DupGroup g : groups) {
                for (int i = 0; i < g.paths.size(); i++) {
                    if (i == g.keepIndex) continue; // 保留选中的那一份
                    if (deleteFile(g.paths.get(i), rootShell, root)) ok++;
                    else fail++;
                }
            }
            if (rootShell != null) rootShell.close();
            final int fOk = ok, fFail = fail;
            runOnUiThread(() -> {
                pb.setVisibility(View.GONE);
                Toast.makeText(this, "清理完成：成功 " + fOk + " 项，失败 " + fFail + " 项", Toast.LENGTH_LONG).show();
                startScan();
            });
        }).start();
    }

    private boolean deleteFile(String path, RootShell root, boolean rootAvail) {
        if (rootAvail && root != null && root.isAvailable()) {
            String r = root.run("rm -f '" + path + "' 2>/dev/null && echo OK || echo FAIL");
            return r.contains("OK");
        }
        return new File(path).delete();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    static class DupGroupAdapter extends RecyclerView.Adapter<DupGroupAdapter.VH> {

        private final List<DuplicateScanner.DupGroup> data;

        DupGroupAdapter(List<DuplicateScanner.DupGroup> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_dup_group, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            DuplicateScanner.DupGroup g = data.get(pos);
            long saveable = g.size * (g.paths.size() - 1);
            h.tvTitle.setText("重复组：共 " + g.paths.size() + " 个，每个 " + Util.formatSize(g.size)
                    + "，可释放 " + Util.formatSize(saveable));
            h.rg.removeAllViews();
            for (int i = 0; i < g.paths.size(); i++) {
                RadioButton rb = new RadioButton(h.rg.getContext());
                rb.setText(g.paths.get(i));
                rb.setChecked(i == g.keepIndex);
                final int idx = i;
                final DuplicateScanner.DupGroup grp = g;
                rb.setOnCheckedChangeListener((btn, checked) -> {
                    if (checked) grp.keepIndex = idx;
                });
                h.rg.addView(rb);
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTitle;
            RadioGroup rg;

            VH(View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tv_group_title);
                rg = v.findViewById(R.id.rg_files);
            }
        }
    }
}
