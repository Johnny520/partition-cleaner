/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 企业查询：调用鲸海数据（kqdaas）开放 API 查询工商信息。
 * 数据来源：https://www.kqdaas.com （实名认证后赠 1000 次免费调用）。
 * 鉴权：HTTP Header X-Jinghai-App-Id + X-Jinghai-Api-Key。
 * 密钥优先级：用户自填(SharedPreferences) > 内置(BuildConfig)。
 */
public class EnterpriseActivity extends BaseActivity {

    private static final String BASE = "https://www.kqdaas.com";
    private static final String API = "/DataService/api/v3/company/detail/";
    private static final String PREFS = "kqdaas_prefs";
    private static final String K_APP_ID = "app_id";
    private static final String K_API_KEY = "api_key";

    private TextInputEditText etQuery;
    private MaterialButton btnSearch, btnSettings, btnHelp;
    private TextView tvStatus;
    private RecyclerView rvResult;
    private ProgressBar progress;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<EntResult> results = new ArrayList<>();
    private ResultAdapter adapter;

    /** 单家企业查询结果（字段已解析为字符串）。 */
    public static class EntResult {
        public String name, legal, capital, establish, status, address, credit;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.feat_company);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etQuery = findViewById(R.id.et_query);
        btnSearch = findViewById(R.id.btn_search);
        btnSettings = findViewById(R.id.btn_settings);
        btnHelp = findViewById(R.id.btn_help);
        tvStatus = findViewById(R.id.tv_status);
        rvResult = findViewById(R.id.rv_result);
        progress = findViewById(R.id.progress);

        adapter = new ResultAdapter(results);
        rvResult.setLayoutManager(new LinearLayoutManager(this));
        rvResult.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> doSearch());
        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
        btnSettings.setOnClickListener(v -> showSettingsDialog());
        btnHelp.setOnClickListener(v -> showGuideDialog());
    }

    /** 用户自填密钥优先，否则用内置开发者密钥。 */
    private String getAppId() {
        String u = getSharedPreferences(PREFS, MODE_PRIVATE).getString(K_APP_ID, "");
        if (!TextUtils.isEmpty(u)) return u;
        return BuildConfig.KQDAAS_APP_ID;
    }

    private String getApiKey() {
        String u = getSharedPreferences(PREFS, MODE_PRIVATE).getString(K_API_KEY, "");
        if (!TextUtils.isEmpty(u)) return u;
        return BuildConfig.KQDAAS_API_KEY;
    }

    private boolean hasUserKey() {
        return !TextUtils.isEmpty(getSharedPreferences(PREFS, MODE_PRIVATE).getString(K_API_KEY, ""));
    }

    private void doSearch() {
        String q = etQuery.getText().toString().trim();
        if (TextUtils.isEmpty(q)) return;

        tvStatus.setVisibility(View.GONE);
        results.clear();
        adapter.notifyDataSetChanged();
        progress.setVisibility(View.VISIBLE);

        String appId = getAppId();
        String apiKey = getApiKey();
        final boolean usingBuiltin = !hasUserKey();

        new Thread(() -> {
            try {
                // 18 位信用代码按代码查，其余按名称查
                int queryType = q.matches("[0-9A-Za-z]{18}") ? 2 : 1;
                String kw = URLEncoder.encode(q, "UTF-8");
                String urlStr = BASE + API + kw + "?queryType=" + queryType;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-Jinghai-App-Id", appId);
                conn.setRequestProperty("X-Jinghai-Api-Key", apiKey);
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                int code = conn.getResponseCode();
                if (code != 200) {
                    conn.disconnect();
                    final String emsg = (code == 401 || code == 403)
                            ? getString(R.string.ent_auth_fail)
                            : getString(R.string.ent_http_fail, code);
                    handler.post(() -> showError(emsg + (usingBuiltin ? "\n" + getString(R.string.ent_use_builtin) : "")));
                    return;
                }

                String body = readStream(conn.getInputStream());
                conn.disconnect();

                EntResult r = parse(body);
                if (r == null) {
                    handler.post(() -> showError(getString(R.string.ent_parse_fail)
                            + (usingBuiltin ? "\n" + getString(R.string.ent_use_builtin) : "")));
                    return;
                }

                handler.post(() -> {
                    progress.setVisibility(View.GONE);
                    results.add(r);
                    adapter.notifyDataSetChanged();
                    if (usingBuiltin) {
                        tvStatus.setVisibility(View.VISIBLE);
                        tvStatus.setText(R.string.ent_use_builtin);
                    }
                });
            } catch (Exception e) {
                handler.post(() -> showError(getString(R.string.ent_network_fail, e.getMessage())));
            }
        }).start();
    }

    /** 解析返回 JSON（兼容 status / errcode 外层标识，兼容多候选字段名）。 */
    private EntResult parse(String body) {
        try {
            JSONObject root = new JSONObject(body);
            boolean ok = root.optInt("status", -1) == 200 || root.optInt("errcode", -1) == 200;
            if (!ok && !root.has("data")) return null; // 明确失败且无解
            JSONObject data = root.optJSONObject("data");
            if (data == null) return null;

            EntResult r = new EntResult();
            r.name = opt(data, "companyName");
            r.legal = opt(data, "juridicalPerson", "legalPerson", "legalRepresentative");
            r.capital = opt(data, "registeredCapital", "regCapital", "capital");
            r.establish = opt(data, "establishTime", "establishDate", "foundDate");
            r.status = opt(data, "businessStatus", "operateStatus", "regStatus");
            r.address = opt(data, "address", "regAddress", "regitAddress", "domicile");
            r.credit = opt(data, "creditNumber", "creditCode", "unifiedSocialCreditCode", "usci");
            return r;
        } catch (Exception e) {
            return null;
        }
    }

    private String opt(JSONObject o, String... keys) {
        for (String k : keys) {
            String v = o.optString(k, null);
            if (v != null && !v.isEmpty() && !"null".equals(v)) return v;
        }
        return "";
    }

    private String readStream(InputStream in) throws Exception {
        if (in == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        return sb.toString();
    }

    private void showError(String msg) {
        progress.setVisibility(View.GONE);
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(msg);
    }

    /** 设置对话框：用户自行填入鲸海 App Id / Api Key（持久化到 SharedPreferences）。 */
    private void showSettingsDialog() {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_kqdaas_key, null);
        EditText etId = v.findViewById(R.id.et_app_id);
        EditText etKey = v.findViewById(R.id.et_api_key);
        etId.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(K_APP_ID, ""));
        etKey.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(K_API_KEY, ""));

        new AlertDialog.Builder(this)
                .setTitle(R.string.ent_settings)
                .setView(v)
                .setPositiveButton(R.string.ent_save, (d, w) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putString(K_APP_ID, etId.getText().toString().trim())
                            .putString(K_API_KEY, etKey.getText().toString().trim())
                            .apply();
                    Toast.makeText(this, R.string.ent_key_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.ent_cancel, null)
                .show();
    }

    /** 申请指引：告诉用户如何注册并拿到 key。 */
    private void showGuideDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ent_apply_title)
                .setMessage(R.string.ent_apply_text)
                .setPositiveButton(R.string.ent_ok, null)
                .show();
    }

    static class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.VH> {
        private final List<EntResult> list;

        ResultAdapter(List<EntResult> l) {
            this.list = l;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_enterprise_result, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            EntResult r = list.get(pos);
            Context ctx = h.itemView.getContext();
            h.tvName.setText(or(r.name));
            h.tvLegal.setText(ctx.getString(R.string.ent_lbl_legal) + "：" + or(r.legal));
            h.tvCapital.setText(ctx.getString(R.string.ent_lbl_capital) + "：" + or(r.capital));
            h.tvEstablish.setText(ctx.getString(R.string.ent_lbl_establish) + "：" + or(r.establish));
            h.tvStatus.setText(ctx.getString(R.string.ent_lbl_status) + "：" + or(r.status));
            h.tvAddress.setText(ctx.getString(R.string.ent_lbl_address) + "：" + or(r.address));
            h.tvCredit.setText(ctx.getString(R.string.ent_lbl_credit) + "：" + or(r.credit));
        }

        private String or(String s) {
            return TextUtils.isEmpty(s) ? "—" : s;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvLegal, tvCapital, tvEstablish, tvStatus, tvAddress, tvCredit;

            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvLegal = v.findViewById(R.id.tv_legal);
                tvCapital = v.findViewById(R.id.tv_capital);
                tvEstablish = v.findViewById(R.id.tv_establish);
                tvStatus = v.findViewById(R.id.tv_status);
                tvAddress = v.findViewById(R.id.tv_address);
                tvCredit = v.findViewById(R.id.tv_credit);
            }
        }
    }
}
