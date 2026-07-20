/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

/** 用户自定义清理规则管理：列表 / 新增 / 编辑 / 删除 / 扫描清理。 */
public class CustomCleanActivity extends BaseActivity {

    public static final String EXTRA_RULE_ID = "rule_id";

    private RecyclerView rv;
    private CustomRuleStore store;
    private final List<CustomRule> rules = new ArrayList<>();
    private RuleAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_clean);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.custom_title);
        }

        store = new CustomRuleStore(this);
        rv = findViewById(R.id.rv_custom);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RuleAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> startActivity(new Intent(this, CustomRuleEditActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        rules.clear();
        rules.addAll(store.getAll());
        adapter.notifyDataSetChanged();
    }

    private void openScan(CustomRule rule) {
        Intent it = new Intent(this, ScanResultActivity.class);
        it.putExtra(ScanResultActivity.EXTRA_CUSTOM_RULE_ID, rule.id);
        startActivity(it);
    }

    private void deleteRule(CustomRule rule) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.custom_delete)
                .setMessage(rule.name)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    store.remove(rule.id);
                    rules.remove(rule);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, R.string.custom_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    class RuleAdapter extends RecyclerView.Adapter<RuleVH> {
        @NonNull
        @Override
        public RuleVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_custom_rule, parent, false);
            return new RuleVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RuleVH h, int pos) {
            CustomRule r = rules.get(pos);
            h.name.setText(r.name);
            h.desc.setText("路径: " + r.rootsText() + "\n模式: " + r.modeText()
                    + " ｜ 匹配: " + r.patternsText() + " ｜ " + r.adviceText());
            h.btnScan.setOnClickListener(v -> openScan(r));
            h.btnEdit.setOnClickListener(v -> {
                Intent it = new Intent(CustomCleanActivity.this, CustomRuleEditActivity.class);
                it.putExtra(EXTRA_RULE_ID, r.id);
                startActivity(it);
            });
            h.btnDelete.setOnClickListener(v -> deleteRule(r));
        }

        @Override
        public int getItemCount() { return rules.size(); }
    }

    class RuleVH extends RecyclerView.ViewHolder {
        TextView name, desc;
        android.widget.Button btnScan, btnEdit, btnDelete;
        RuleVH(View v) {
            super(v);
            name = v.findViewById(R.id.tv_rule_name);
            desc = v.findViewById(R.id.tv_rule_desc);
            btnScan = v.findViewById(R.id.btn_scan);
            btnEdit = v.findViewById(R.id.btn_edit);
            btnDelete = v.findViewById(R.id.btn_delete);
        }
    }
}
