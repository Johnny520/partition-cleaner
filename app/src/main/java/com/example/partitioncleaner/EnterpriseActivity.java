package com.example.partitioncleaner;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 企业查询：多源免费爬虫聚合。
 * 通过应用内隐藏 WebView 依次加载多家公开工商信息网站的搜索结果页，
 * 用 JS 抽取列表项文本，聚合并去重后展示，每张卡片标注数据来源。
 * 无需任何 API Key。某源被反爬/超时则优雅跳过，其余源继续。
 */
public class EnterpriseActivity extends BaseActivity {

    private static final String TAG = "Enterprise";
    private static final String PREFS = "ent_src_prefs";
    private static final long SOURCE_TIMEOUT_MS = 12000;
    private static final long EXTRACT_DELAY_MS = 1800;
    private static final String DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** 多源聚合：隐藏 WebView 依次爬取公开工商信息网站（按优先级顺序回退）。 */
    private static final Source[] SOURCES = new Source[]{
            new Source("aiqicha", R.string.ent_src_aiqicha,
                    "https://aiqicha.baidu.com/s?q=%s",
                    new String[]{"div.result-item", "div.list-item", "li[class*='item']", "div[class*='result']"},
                    true, null),
            new Source("qcc", R.string.ent_src_qcc,
                    "https://www.qcc.com/web/search?key=%s",
                    new String[]{"div.company-item", "div[class*='company']", "div.result-item", "li[class*='item']"},
                    true, null),
            new Source("tyc", R.string.ent_src_tyc,
                    "https://www.tianyancha.com/search?key=%s",
                    new String[]{"div.company-item", "div[class*='company']", "div.result-item", "li[class*='item']"},
                    true, null),
            new Source("gsxt", R.string.ent_src_gsxt,
                    "https://www.gsxt.gov.cn/corp-query-search-1.html?key=%s",
                    new String[]{"table.list-table tr", "tr", "div[class*='list']", "div[class*='item']"},
                    true, null),
            new Source("qixin", R.string.ent_src_qixin,
                    "https://www.qixin.com/search?key=%s",
                    new String[]{"div[class*='item']", "div[class*='result']", "li[class*='item']", "div[class*='company']"},
                    true, null),
            new Source("shuidi", R.string.ent_src_shuidi,
                    "https://www.shuidi.cn/search?key=%s",
                    new String[]{"div[class*='item']", "div[class*='result']", "li[class*='item']", "div[class*='company']"},
                    true, null),
            new Source("qichamao", R.string.ent_src_qichamao,
                    "https://www.qichamao.com/search?key=%s",
                    new String[]{"div[class*='item']", "div[class*='result']", "li[class*='item']", "div[class*='company']"},
                    true, null),
    };

    /** JS 抽取模板：%s 注入选择器数组字面量，返回结果对象数组 [{t:标题, x:正文}]。 */
    private static final String JS_TPL =
            "function(){var sels=%s;function clean(s){return (s||'').replace(/\\s+/g,' ').trim();}"
                    + "var nodes=[];for(var i=0;i<sels.length;i++){var n=document.querySelectorAll(sels[i]);"
                    + "if(n&&n.length){nodes=Array.prototype.slice.call(n);break;}}"
                    + "var out=[];for(var j=0;j<nodes.length&&j<25;j++){var el=nodes[j];"
                    + "var a=el.querySelector?el.querySelector('a'):null;"
                    + "var title=clean(a?a.innerText:'')||clean(el.innerText).split(' ')[0];"
                    + "var text=clean(el.innerText);if(text.length>4)out.push({t:title,x:text});}"
                    + "return out;}";

    /** 通用选择器（自定义源与兜底使用）。 */
    private static final String[] DEFAULT_SELECTORS = new String[]{
            "div[class*='item']", "div[class*='result']", "li[class*='item']",
            "div[class*='company']", "tr", "a[class*='name']"
    };

    static class Source {
        final String key;
        final int nameRes;
        final String base;
        final String[] selectors;
        final boolean defaultEnabled;
        final String selectorsLit;
        String displayName;   // 自定义源使用（优先于 nameRes）
        String apiKey;        // 自定义源可选 Key

        Source(String k, int n, String b, String[] s, boolean d, String apiKey) {
            key = k;
            nameRes = n;
            base = b;
            selectors = s;
            defaultEnabled = d;
            this.apiKey = apiKey;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < s.length; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(s[i]).append("\"");
            }
            sb.append("]");
            selectorsLit = sb.toString();
        }

        String searchUrl(String kw) throws Exception {
            String u = String.format(Locale.US, base, URLEncoder.encode(kw, "UTF-8"));
            if (!TextUtils.isEmpty(apiKey)) {
                u += (u.contains("?") ? "&" : "?") + "key=" + URLEncoder.encode(apiKey, "UTF-8");
            }
            return u;
        }
    }

    static class EntResult {
        final int nameRes;
        final String title;
        final String body;
        final String url;

        EntResult(int nr, String t, String b, String u) {
            nameRes = nr;
            title = t;
            body = b;
            url = u;
        }
    }

    private WebView web;
    private EditText etQuery;
    private TextView tvStatus;
    private RecyclerView rv;
    private android.widget.ProgressBar progressBar;
    private View btnSearch, btnSettings, btnHelp;

    private final List<EntResult> results = new ArrayList<>();
    private ResultAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> usedSourceNames = new HashSet<>();

    private long searchToken = 0;
    private long currentLoadToken = -1;
    private List<Source> queue = new ArrayList<>();
    private int queueIdx = 0;
    private Source currentSource;
    private boolean awaitingExtract = false;
    private String keyword = "";

    private final Runnable extractRunnable = () -> extractCurrent(searchToken);

    private final Runnable timeoutRunnable = () -> {
        if (!awaitingExtract) return;
        awaitingExtract = false;
        if (currentSource != null) {
            setStatus(getString(R.string.ent_status_timeout, srcName(currentSource)));
        }
        queueIdx++;
        loadNext(searchToken);
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enterprise);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.ent_title);
        }

        etQuery = findViewById(R.id.et_query);
        btnSearch = findViewById(R.id.btn_search);
        btnSettings = findViewById(R.id.btn_settings);
        btnHelp = findViewById(R.id.btn_help);
        tvStatus = findViewById(R.id.tv_status);
        rv = findViewById(R.id.rv_result);
        progressBar = findViewById(R.id.progress);

        adapter = new ResultAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        setupWebView();

        btnSearch.setOnClickListener(v -> startSearch());
        btnSettings.setOnClickListener(v -> showSettings());
        btnHelp.setOnClickListener(v -> showHelp());
        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                startSearch();
                return true;
            }
            return false;
        });

        setStatus(getString(R.string.ent_status_ready));
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        web = findViewById(R.id.web);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setUserAgentString(DESKTOP_UA);
        ws.setCacheMode(WebSettings.LOAD_DEFAULT);
        web.setWebViewClient(new ScrapeClient());
        web.setBackgroundColor(Color.TRANSPARENT);
    }

    private void startSearch() {
        keyword = etQuery.getText().toString().trim();
        if (TextUtils.isEmpty(keyword)) {
            etQuery.setError("请输入企业名称或信用代码");
            return;
        }
        results.clear();
        usedSourceNames.clear();
        adapter.notifyDataSetChanged();

        List<Source> selected = getEnabledSources();
        if (selected.isEmpty()) {
            setSearching(false);
            setStatus(getString(R.string.ent_status_no_source));
            return;
        }

        queue = selected;
        queueIdx = 0;
        searchToken++;
        long token = searchToken;
        handler.removeCallbacks(extractRunnable);
        handler.removeCallbacks(timeoutRunnable);
        web.stopLoading();
        setSearching(true);
        loadNext(token);
    }

    private void loadNext(long token) {
        if (token != searchToken) return;
        if (queueIdx >= queue.size()) {
            finishSearch(token);
            return;
        }
        currentSource = queue.get(queueIdx);
        awaitingExtract = true;
        currentLoadToken = token;
        setStatus(getString(R.string.ent_status_searching,
                srcName(currentSource), queueIdx + 1, queue.size()));
        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, SOURCE_TIMEOUT_MS);
        try {
            web.loadUrl(currentSource.searchUrl(keyword));
        } catch (Exception e) {
            awaitingExtract = false;
            setStatus(getString(R.string.ent_status_err, e.getMessage()));
            queueIdx++;
            loadNext(token);
        }
    }

    private class ScrapeClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (currentLoadToken != searchToken) return;
            handler.removeCallbacks(extractRunnable);
            handler.postDelayed(extractRunnable, EXTRACT_DELAY_MS);
        }
    }

    private void extractCurrent(long token) {
        if (token != searchToken) return;
        if (!awaitingExtract) return;
        awaitingExtract = false;
        handler.removeCallbacks(timeoutRunnable);

        final Source src = currentSource;
        final String js = String.format(Locale.US, JS_TPL, src.selectorsLit);
        web.evaluateJavascript(js, value -> {
            if (token != searchToken) return;
            int added = mergeResults(src, value);
            if (added > 0) {
                usedSourceNames.add(srcName(src));
                setStatus(getString(R.string.ent_status_found, srcName(src), added));
            } else {
                setStatus(getString(R.string.ent_status_skip, srcName(src)));
            }
            queueIdx++;
            loadNext(token);
        });
    }

    private int mergeResults(Source src, String value) {
        int before = results.size();
        try {
            JSONArray arr = new JSONArray(value);
            String url = src.searchUrl(keyword);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String t = o.optString("t", "");
                String x = o.optString("x", "");
                if (TextUtils.isEmpty(x)) continue;
                results.add(new EntResult(src.nameRes, t, x, url));
            }
        } catch (Exception e) {
            Log.w(TAG, "parse fail: " + e.getMessage());
        }
        return results.size() - before;
    }

    private void finishSearch(long token) {
        if (token != searchToken) return;
        handler.removeCallbacks(extractRunnable);
        handler.removeCallbacks(timeoutRunnable);
        setSearching(false);
        if (results.isEmpty()) {
            setStatus(getString(R.string.ent_status_empty));
        } else {
            setStatus(getString(R.string.ent_status_done, results.size(),
                    TextUtils.join("、", new ArrayList<>(usedSourceNames))));
        }
        adapter.notifyDataSetChanged();
    }

    private void setSearching(boolean b) {
        progressBar.setVisibility(b ? View.VISIBLE : View.GONE);
        btnSearch.setEnabled(!b);
    }

    private void setStatus(String s) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(s);
    }

    private String srcName(Source s) {
        return s.displayName != null ? s.displayName : getString(s.nameRes);
    }

    private List<Source> getEnabledSources() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        List<Source> list = new ArrayList<>();
        for (Source s : SOURCES) {
            if (sp.getBoolean("enabled_" + s.key, s.defaultEnabled)) {
                list.add(s);
            }
        }
        // 自定义 API 源（若已启用且配置了网址）
        if (sp.getBoolean("custom_enabled", false)) {
            String url = sp.getString("custom_url", "");
            if (!TextUtils.isEmpty(url)) {
                Source cs = new Source("custom", 0, url, DEFAULT_SELECTORS, true,
                        sp.getString("custom_key", ""));
                cs.displayName = sp.getString("custom_name", getString(R.string.ent_custom_source));
                list.add(cs);
            }
        }
        return list;
    }

    private void showSettings() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean customOn = sp.getBoolean("custom_enabled", false);
        String customUrl = sp.getString("custom_url", "");
        boolean hasCustom = !TextUtils.isEmpty(customUrl);

        View root = LayoutInflater.from(this).inflate(R.layout.dialog_source_settings, null);
        Button btnCustom = root.findViewById(R.id.btn_custom);
        LinearLayout container = root.findViewById(R.id.container_sources);

        // 内置源复选框（默认全选，用户可取消）
        final List<CheckBox> boxes = new ArrayList<>();
        for (int i = 0; i < SOURCES.length; i++) {
            CheckBox cb = new CheckBox(this);
            cb.setText(getString(SOURCES[i].nameRes));
            cb.setChecked(sp.getBoolean("enabled_" + SOURCES[i].key, SOURCES[i].defaultEnabled));
            cb.setTextSize(15);
            container.addView(cb);
            boxes.add(cb);
        }

        // 自定义源复选框（若已配置）
        final CheckBox cbCustom;
        if (hasCustom) {
            cbCustom = new CheckBox(this);
            cbCustom.setText(sp.getString("custom_name", getString(R.string.ent_custom_source)));
            cbCustom.setChecked(customOn);
            cbCustom.setTextSize(15);
            container.addView(cbCustom);
        } else {
            cbCustom = null;
        }

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(R.string.ent_settings_title)
                .setView(root)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    SharedPreferences.Editor e = sp.edit();
                    for (int i = 0; i < SOURCES.length; i++) {
                        e.putBoolean("enabled_" + SOURCES[i].key, boxes.get(i).isChecked());
                    }
                    if (cbCustom != null) e.putBoolean("custom_enabled", cbCustom.isChecked());
                    e.apply();
                    Toast.makeText(this, R.string.ent_ok, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();

        btnCustom.setOnClickListener(v -> {
            dlg.dismiss();
            showCustomSourceDialog();
        });
    }

    /** 添加 / 修改自定义 API 源：名称 + 搜索网址(用 %s 代替关键词) + 可选 Key。 */
    private void showCustomSourceDialog() {
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_custom_source, null);
        EditText etName = v.findViewById(R.id.et_custom_name);
        EditText etUrl = v.findViewById(R.id.et_custom_url);
        EditText etKey = v.findViewById(R.id.et_custom_key);
        etName.setText(sp.getString("custom_name", ""));
        etUrl.setText(sp.getString("custom_url", ""));
        etKey.setText(sp.getString("custom_key", ""));

        new AlertDialog.Builder(this)
                .setTitle(R.string.ent_custom_title)
                .setMessage(R.string.ent_custom_summary)
                .setView(v)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String url = etUrl.getText().toString().trim();
                    String key = etKey.getText().toString().trim();
                    if (TextUtils.isEmpty(url)) {
                        Toast.makeText(this, R.string.ent_custom_url_hint, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String finalName = name.isEmpty() ? getString(R.string.ent_custom_source) : name;
                    SharedPreferences.Editor e = sp.edit();
                    e.putString("custom_name", finalName);
                    e.putString("custom_url", url);
                    e.putString("custom_key", key);
                    e.putBoolean("custom_enabled", true);
                    e.apply();
                    Toast.makeText(this, getString(R.string.ent_custom_saved, finalName), Toast.LENGTH_SHORT).show();
                    showSettings();
                })
                .setNeutralButton(R.string.ent_custom_remove, (d, w) -> {
                    SharedPreferences.Editor e = sp.edit();
                    e.remove("custom_name");
                    e.remove("custom_url");
                    e.remove("custom_key");
                    e.putBoolean("custom_enabled", false);
                    e.apply();
                    showSettings();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.ent_help_title)
                .setMessage(R.string.ent_help_text)
                .setPositiveButton(R.string.ent_ok, null)
                .show();
    }

    private void openUrl(String u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.ent_open_fail, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
        }
        super.onDestroy();
    }

    class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int t) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_enterprise_result, p, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            EntResult r = results.get(pos);
            h.tvSource.setText(getString(r.nameRes));
            h.tvName.setText(TextUtils.isEmpty(r.title) ? getString(R.string.ent_no_title) : r.title);
            h.tvBody.setText(r.body);
            h.tvLink.setText(getString(R.string.ent_open_web, getString(r.nameRes)));
            h.tvLink.setOnClickListener(v -> openUrl(r.url));
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView tvSource, tvName, tvBody, tvLink;

            VH(View v) {
                super(v);
                tvSource = v.findViewById(R.id.tv_source);
                tvName = v.findViewById(R.id.tv_name);
                tvBody = v.findViewById(R.id.tv_body);
                tvLink = v.findViewById(R.id.tv_link);
            }
        }
    }
}
