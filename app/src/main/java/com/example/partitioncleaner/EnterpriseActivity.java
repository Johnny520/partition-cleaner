/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 企业查询：内嵌国家企业信用信息公示系统（gsxt.gov.cn）。
 * 该站有 JS 挑战(WAF) + AJAX 动态渲染，直爬/requests 拿不到数据，
 * 故用 WebView（真实浏览器）加载官方页面取数，再注入 JS 提取企业列表，
 * 以原生 Material 卡片展示（归类/去重/排序/可点击），WebView 同时作为详情兜底。
 */
public class EnterpriseActivity extends BaseActivity {

    private static final String GSXT = "https://www.gsxt.gov.cn/";

    /** 去除新窗口打开，强制在当前 WebView 内跳转（避免详情页开新窗口丢失）。 */
    private static final String FIX_LINKS_JS = "(function(){"
            + "var as=document.querySelectorAll('a');"
            + "for(var i=0;i<as.length;i++){as[i].removeAttribute('target');}"
            + "})()";

    /** 轻量美化：去掉官方页面多余外边距，适配手机宽度。 */
    private static final String CSS_JS = "(function(){"
            + "var s=document.createElement('style');"
            + "s.textContent='html,body{margin:0!important;padding:0!important;}body{font-size:15px;max-width:100%!important;overflow-x:hidden!important;}';"
            + "if(document.head)document.head.appendChild(s);"
            + "})()";

    /** 提取企业列表：遍历 a 标签，过滤非企业/导航，返回 [{name,url}]。 */
    private static final String EXTRACT_JS = "(function(){try{"
            + "var links=document.querySelectorAll('a');"
            + "var items=[];var seen={};"
            + "var bad=/首页|登录|注册|帮助|返回|上一页|下一页|关于|联系|版权|备案|使用帮助|业务咨询|小程序|APP|隐私|免责|主办|邮箱|电话|地址|扫一扫|关注|English|无障碍/;"
            + "var corp=/公司|集团|企业|厂|店|银行|股份|有限公司|责任|合伙|事务所|合作社|工作室|医院|学校|大学|学院|中心|局|委|厅|协会|基金会/;"
            + "for(var i=0;i<links.length;i++){"
            + "var a=links[i];"
            + "var text=(a.innerText||a.textContent||'').replace(/\\s+/g,'').trim();"
            + "var href=a.getAttribute('href')||a.href||'';"
            + "if(!text||!href)continue;"
            + "if(href.indexOf('javascript:')===0)continue;"
            + "if(seen[text])continue;"
            + "if(bad.test(text))continue;"
            + "if(corp.test(text)&&text.length>=2&&text.length<=40){items.push({name:text,url:href});seen[text]=1;}"
            + "}"
            + "return items;"
            + "}catch(e){return [];}"
            + "})()";

    private TextInputEditText etQuery;
    private MaterialButton btnSearch;
    private TextView tvStatus;
    private WebView web;
    private RecyclerView rvCards;
    private ProgressBar progress;
    private FloatingActionButton fabView;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<EntItem> items = new ArrayList<>();
    private EnterpriseCardAdapter adapter;
    private boolean cardView = false;
    private String pendingQuery = null;

    /** 提取出的企业条目（结构化的原生数据）。 */
    public static class EntItem {
        public String name;
        public String url;
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
        tvStatus = findViewById(R.id.tv_status);
        web = findViewById(R.id.web);
        rvCards = findViewById(R.id.rv_cards);
        progress = findViewById(R.id.progress);
        fabView = findViewById(R.id.fab_view);

        setupWebView();
        adapter = new EnterpriseCardAdapter(items, this::openDetail);
        rvCards.setLayoutManager(new LinearLayoutManager(this));
        rvCards.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> doSearch());
        etQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });
        fabView.setOnClickListener(v -> toggleView());

        loadHome();
    }

    private void setupWebView() {
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setLoadWithOverviewMode(true);
        ws.setUseWideViewPort(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        // 桌面 UA，规避移动端 WebView 检测/不同布局
        ws.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        CookieManager.getInstance().setAcceptCookie(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false; // 当前 WebView 内打开
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                showProgress(true);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                showProgress(false);
                // 页面就绪后先修复链接 + 美化，再决定搜索或提取
                web.evaluateJavascript(FIX_LINKS_JS, null);
                web.evaluateJavascript(CSS_JS, null);
                if (pendingQuery != null) {
                    String q = pendingQuery;
                    pendingQuery = null;
                    web.evaluateJavascript(buildSearchJs(q), null);
                    return;
                }
                // 结果页多为 AJAX 异步渲染，延时再提取
                handler.postDelayed(() -> extractAndShow(), 1500);
            }
        });
    }

    private void loadHome() {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.company_loading_site);
        web.loadUrl(GSXT);
    }

    private void doSearch() {
        String q = etQuery.getText().toString().trim();
        if (q.isEmpty()) return;
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.company_loading);
        if (web.getUrl() == null || !web.getUrl().contains("gsxt.gov.cn")) {
            pendingQuery = q;
            web.loadUrl(GSXT);
        } else {
            web.evaluateJavascript(buildSearchJs(q), null);
        }
    }

    /** 在 WebView 内填充搜索框并提交（兼容 gsxt 动态表单，无需逆向接口）。 */
    private String buildSearchJs(String q) {
        String esc = q.replace("\\", "\\\\").replace("'", "\\'");
        return "(function(){"
                + "var q='" + esc + "';"
                + "var inputs=document.querySelectorAll(\"input[type='text'],input[type='search'],input:not([type])\");"
                + "var inp=null;"
                + "for(var i=0;i<inputs.length;i++){var p=(inputs[i].placeholder||'')+(inputs[i].name||'')+(inputs[i].id||'');if(/企业|信用|查询|搜索|关键字|key|ent|name/i.test(p)){inp=inputs[i];break;}}"
                + "if(!inp&&inputs.length)inp=inputs[0];"
                + "if(inp){inp.value=q;inp.dispatchEvent(new Event('input',{bubbles:true}));"
                + "var btns=document.querySelectorAll('button,input[type=submit],a.btn');"
                + "for(var j=0;j<btns.length;j++){var t=(btns[j].innerText||btns[j].value||btns[j].textContent||'');if(/查询|搜索|搜一下|search|submit/i.test(t)){btns[j].click();return 'ok';}}"
                + "var ev=new KeyboardEvent('keydown',{key:'Enter',keyCode:13,bubbles:true});inp.dispatchEvent(ev);return 'enter';}"
                + "return 'no_input';"
                + "})()";
    }

    private void extractAndShow() {
        web.evaluateJavascript(EXTRACT_JS, value -> {
            if (value == null) return;
            parseAndShow(value);
        });
    }

    private void parseAndShow(String json) {
        try {
            JSONArray arr = new JSONArray(json);
            if (arr.length() == 0) return;
            items.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                EntItem it = new EntItem();
                it.name = o.optString("name");
                it.url = o.optString("url");
                if (!TextUtils.isEmpty(it.name)) items.add(it);
            }
            // 归类/排序/去重（去重在 JS 侧已完成）：按名称优化排列
            Collections.sort(items, (a, b) -> a.name.compareToIgnoreCase(b.name));
            handler.post(() -> {
                adapter.notifyDataSetChanged();
                if (!cardView) toggleView();
            });
        } catch (Exception ignored) {
            // 提取失败不影响 WebView 直接使用
        }
    }

    private void toggleView() {
        cardView = !cardView;
        if (cardView) {
            rvCards.setVisibility(View.VISIBLE);
            web.setVisibility(View.GONE);
            if (items.isEmpty()) {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(R.string.company_no_result);
            } else {
                tvStatus.setVisibility(View.VISIBLE);
                tvStatus.setText(getString(R.string.company_card_count, items.size()));
            }
        } else {
            rvCards.setVisibility(View.GONE);
            web.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.GONE);
        }
    }

    private void openDetail(EntItem it) {
        if (it.url != null && !it.url.isEmpty()) {
            cardView = false;
            rvCards.setVisibility(View.GONE);
            web.setVisibility(View.VISIBLE);
            tvStatus.setVisibility(View.GONE);
            web.loadUrl(it.url);
        } else {
            Toast.makeText(this, R.string.company_open_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgress(boolean show) {
        progress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && web.canGoBack()
                && web.getVisibility() == View.VISIBLE) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
