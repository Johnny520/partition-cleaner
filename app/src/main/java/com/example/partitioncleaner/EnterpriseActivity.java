package com.example.partitioncleaner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class EnterpriseActivity extends BaseActivity {

    private EditText etQuery;
    private MaterialButton btnSearch;
    private TextView tvStatus;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private final List<String> names = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private CompanyFetcher fetcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.feat_company);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fetcher = new CompanyFetcher(this);

        etQuery = findViewById(R.id.et_query);
        btnSearch = findViewById(R.id.btn_search);
        tvStatus = findViewById(R.id.tv_status);
        listView = findViewById(R.id.list_results);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> doSearch());
        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String name = names.get(position);
            if (name != null) openDetail(name);
        });
    }

    private void doSearch() {
        String q = etQuery.getText().toString().trim();
        if (q.isEmpty()) return;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.company_loading);
        listView.setVisibility(View.GONE);
        new Thread(() -> {
            final List<String> list = fetcher.search(q);
            handler.post(() -> {
                if (list.isEmpty()) {
                    tvStatus.setText(R.string.company_no_result);
                    listView.setVisibility(View.GONE);
                } else {
                    tvStatus.setVisibility(View.GONE);
                    listView.setVisibility(View.VISIBLE);
                    names.clear();
                    names.addAll(list);
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void openDetail(String name) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.company_loading);
        new Thread(() -> {
            final CompanyFetcher.Company c = fetcher.getDetail(name);
            handler.post(() -> {
                tvStatus.setVisibility(View.GONE);
                showDetail(c);
            });
        }).start();
    }

    private void showDetail(CompanyFetcher.Company c) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_enterprise_detail, null);
        TextView tvName = v.findViewById(R.id.tv_d_name);
        TextView tvInfo = v.findViewById(R.id.tv_d_info);
        MaterialButton btnVerify = v.findViewById(R.id.btn_verify);
        MaterialButton btnSite = v.findViewById(R.id.btn_site);

        tvName.setText(c.name);
        StringBuilder sb = new StringBuilder();
        if (c.creditCode != null) sb.append("统一社会信用代码：").append(c.creditCode).append("\n");
        if (c.legalPerson != null) sb.append("法定代表人：").append(c.legalPerson).append("\n");
        if (c.registeredCapital != null) sb.append("注册资本：").append(c.registeredCapital).append("\n");
        if (c.establishDate != null) sb.append("成立日期：").append(c.establishDate).append("\n");
        if (c.status != null) sb.append("登记状态：").append(c.status).append("\n");
        if (c.regAddress != null) sb.append("注册地址：").append(c.regAddress).append("\n");
        if (!c.shareholders.isEmpty()) sb.append("股东：").append(TextUtils.join("、", c.shareholders)).append("\n");
        if (c.tip != null) sb.append("\n").append(c.tip);
        tvInfo.setText(sb.toString());

        if (c.website != null) {
            btnSite.setVisibility(View.VISIBLE);
            btnSite.setOnClickListener(x -> openUrl("https://" + c.website));
        } else {
            btnSite.setVisibility(View.GONE);
        }
        btnVerify.setOnClickListener(x ->
                openUrl("https://www.bing.com/search?q=" + Uri.encode(c.name + " 官网")));

        new AlertDialog.Builder(this)
                .setView(v)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.company_open_fail, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
