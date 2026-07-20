/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.app.ActivityManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 内存加速：列出第三方/后台进程，一键 kill 释放内存（需 KILL_BACKGROUND_PROCESSES）。 */
public class MemoryBoostActivity extends BaseActivity {

    private RecyclerView rv;
    private TextView tvMemFree;
    private ActivityManager am;
    private final List<ProcItem> procs = new ArrayList<>();
    private final Set<String> checked = new HashSet<>();
    private ProcAdapter adapter;

    static class ProcItem {
        String pkg;
        String label;
        boolean running;
        ProcItem(String pkg, String label, boolean running) {
            this.pkg = pkg;
            this.label = label;
            this.running = running;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_boost);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.memboost_title);
        }

        am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        tvMemFree = findViewById(R.id.tv_mem_free);
        rv = findViewById(R.id.rv_proc);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProcAdapter();
        rv.setAdapter(adapter);

        MaterialButton btn = findViewById(R.id.btn_boost);
        btn.setOnClickListener(v -> boost());

        load();
    }

    private void load() {
        procs.clear();
        checked.clear();
        updateMem();

        Set<String> running = new HashSet<>();
        try {
            for (ActivityManager.RunningAppProcessInfo p : am.getRunningAppProcesses()) {
                if (p.pkgList != null) {
                    for (String pkg : p.pkgList) running.add(pkg);
                }
            }
        } catch (Exception ignored) {}

        List<ProcItem> items = new ArrayList<>();
        try {
            PackageManager pm = getPackageManager();
            for (ApplicationInfo ai : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (ai.packageName.equals(getPackageName())) continue;
                String label = pm.getApplicationLabel(ai).toString();
                items.add(new ProcItem(ai.packageName, label, running.contains(ai.packageName)));
                checked.add(ai.packageName);
            }
        } catch (Exception ignored) {}
        items.sort((a, b) -> Boolean.compare(!b.running, !a.running));
        procs.addAll(items);
        adapter.notifyDataSetChanged();

        if (running.isEmpty()) {
            Toast.makeText(this, R.string.memboost_limit, Toast.LENGTH_LONG).show();
        }
    }

    private void updateMem() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        tvMemFree.setText(getString(R.string.memboost_free, Formatter.formatFileSize(this, mi.availMem)));
    }

    private void boost() {
        int cnt = 0;
        for (ProcItem p : procs) {
            if (!checked.contains(p.pkg)) continue;
            try {
                am.killBackgroundProcesses(p.pkg);
                cnt++;
            } catch (Exception ignored) {}
        }
        updateMem();
        Toast.makeText(this, getString(R.string.memboost_done, cnt), Toast.LENGTH_SHORT).show();
        load();
    }

    class ProcAdapter extends RecyclerView.Adapter<ProcVH> {
        @NonNull
        @Override
        public ProcVH onCreateViewHolder(@NonNull ViewGroup parent, int vt) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            return new ProcVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ProcVH h, int pos) {
            ProcItem p = procs.get(pos);
            h.tv.setText((p.running ? "● " : "○ ") + p.label + " (" + p.pkg + ")");
            h.itemView.setSelected(checked.contains(p.pkg));
            h.itemView.setOnClickListener(v -> {
                if (checked.contains(p.pkg)) checked.remove(p.pkg);
                else checked.add(p.pkg);
                notifyItemChanged(pos);
            });
        }

        @Override
        public int getItemCount() { return procs.size(); }
    }

    class ProcVH extends RecyclerView.ViewHolder {
        TextView tv;
        ProcVH(View v) {
            super(v);
            tv = v.findViewById(android.R.id.text1);
        }
    }
}
