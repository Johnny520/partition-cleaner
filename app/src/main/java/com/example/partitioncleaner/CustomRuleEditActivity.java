package com.example.partitioncleaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** 新增 / 编辑一条自定义清理规则。 */
public class CustomRuleEditActivity extends BaseActivity {

    private static final long MB = 1024L * 1024L;

    private CustomRuleStore store;
    private CustomRule editing;

    private EditText etName, etRoots, etPatterns, etMinSize, etReason;
    private Spinner spMode, spAdvice;
    private TextView tvPatternsLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_rule_edit);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.custom_add);
        }

        store = new CustomRuleStore(this);
        etName = findViewById(R.id.et_name);
        etRoots = findViewById(R.id.et_roots);
        etPatterns = findViewById(R.id.et_patterns);
        etMinSize = findViewById(R.id.et_minsize);
        etReason = findViewById(R.id.et_reason);
        spMode = findViewById(R.id.sp_mode);
        spAdvice = findViewById(R.id.sp_advice);
        tvPatternsLabel = findViewById(R.id.tv_patterns_label);

        ArrayAdapter<CharSequence> modeAd = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"按目录名匹配", "按文件后缀匹配", "空文件"});
        modeAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMode.setAdapter(modeAd);
        spMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                boolean empty = pos == 2;
                etPatterns.setEnabled(!empty);
                tvPatternsLabel.setEnabled(!empty);
            }
            @Override
            public void onNothingSelected(AdapterView<?> p) {}
        });

        ArrayAdapter<CharSequence> advAd = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"建议清理", "建议保留"});
        advAd.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAdvice.setAdapter(advAd);

        String rid = getIntent().getStringExtra(CustomCleanActivity.EXTRA_RULE_ID);
        if (rid != null) editing = store.getById(rid);
        if (editing != null) {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(R.string.custom_edit);
            fill(editing);
        } else {
            etRoots.setText("/storage/emulated/0/Android/data");
        }

        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setOnClickListener(v -> save());
    }

    private void fill(CustomRule r) {
        etName.setText(r.name);
        etRoots.setText(r.rootsText());
        spMode.setSelection(r.mode);
        etPatterns.setText(r.mode == CustomRule.MODE_EMPTY ? "" : r.patternsText());
        etMinSize.setText(String.valueOf(r.minSize / MB));
        spAdvice.setSelection(r.advice == JunkItem.ADVICE_KEEP ? 1 : 0);
        etReason.setText(r.reason);
    }

    private void save() {
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, R.string.custom_err_name, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] roots = split(etRoots.getText().toString());
        if (roots.length == 0) {
            Toast.makeText(this, R.string.custom_err_roots, Toast.LENGTH_SHORT).show();
            return;
        }
        int mode = spMode.getSelectedItemPosition();
        String[] patterns = mode == CustomRule.MODE_EMPTY
                ? new String[0] : split(etPatterns.getText().toString());
        long minSize = 0;
        try {
            minSize = (long) (Double.parseDouble(etMinSize.getText().toString().trim()) * MB);
        } catch (Exception e) { minSize = 0; }
        int advice = spAdvice.getSelectedItemPosition() == 1
                ? JunkItem.ADVICE_KEEP : JunkItem.ADVICE_CLEAN;
        String reason = etReason.getText().toString().trim();

        if (editing != null) {
            editing.name = name;
            editing.roots = roots;
            editing.mode = mode;
            editing.patterns = patterns;
            editing.minSize = minSize;
            editing.advice = advice;
            editing.reason = reason;
            store.update(editing);
        } else {
            store.add(new CustomRule(name, roots, mode, patterns, minSize, advice, reason));
        }
        Toast.makeText(this, R.string.custom_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private static String[] split(String s) {
        if (s == null || s.trim().isEmpty()) return new String[0];
        String[] parts = s.split("[,，\\n]");
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            p = p.trim();
            if (!p.isEmpty()) out.add(p);
        }
        return out.toArray(new String[0]);
    }
}
