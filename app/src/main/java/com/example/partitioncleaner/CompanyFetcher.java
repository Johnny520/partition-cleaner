package com.example.partitioncleaner;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 企业数据抓取：默认走 Bing 网页抓取兜底（免费、无需 API Key，对标 qixincha-android 的 CompanyFetcher）。
 * 所有异常都被吞掉降级，绝不让调用方崩溃。
 */
public class CompanyFetcher {
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
    private static final int TIMEOUT = 12000;
    private static final String SEARCH = "https://www.bing.com/search?q=";

    private final SharedPreferences cache;
    private final String defaultTip;

    public CompanyFetcher(Context ctx) {
        this.cache = ctx.getApplicationContext().getSharedPreferences("company_cache", Context.MODE_PRIVATE);
        String tip;
        try {
            tip = ctx.getApplicationContext().getString(R.string.company_tip_default);
        } catch (Exception e) {
            tip = "数据来自公开网页抓取（免费、无需密钥），仅供参考，请以官方公示为准。";
        }
        this.defaultTip = tip;
    }

    private String getHtml(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code != 200) {
                conn.disconnect();
                return null;
            }
            InputStream in = conn.getInputStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            r.close();
            conn.disconnect();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String stripTags(String s) {
        return s.replaceAll("<[^>]+>", "")
                .replace("&amp;", "&").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&nbsp;", " ").trim();
    }

    /** 去掉「 - 天眼查」「_百度百科」等站点后缀 */
    private String cleanTitle(String t) {
        if (t == null) return "";
        String[] seps = {" - ", " _ ", " | ", " — ", "－", "–"};
        for (String sep : seps) {
            int idx = t.lastIndexOf(sep);
            if (idx > 0) {
                String right = t.substring(idx + sep.length());
                if (right.matches(".*(天眼查|爱企查|企查查|百度百科|百度知道|维基百科|官网|市场监督管理局|启信宝|水滴信用|企业预警通).*")) {
                    t = t.substring(0, idx);
                }
            }
        }
        return t.trim();
    }

    private boolean looksLikeCompany(String t) {
        if (t == null) return false;
        if (t.length() < 2 || t.length() > 40) return false;
        if (!t.matches(".*[\\u4e00-\\u9fa5]+.*")) return false; // 必须含中文
        return t.matches(".*(公司|集团|企业|厂|店|银行|股份|有限公司|责任|合伙|事务所|医院|学校|大学|学院|中心|合作社|工作室).*");
    }

    /** 从搜索结果 HTML 中尽量抽取企业官网域名（排除搜索引擎/聚合站点）。 */
    private String pickWebsite(String html) {
        try {
            Matcher m = Pattern.compile(
                    "<cite>\\s*([a-z0-9.-]+\\.(?:com|cn|net|org|com\\.cn|gov\\.cn))\\s*</cite>",
                    Pattern.CASE_INSENSITIVE).matcher(html);
            String[] bad = {"bing", "baidu", "tianyancha", "aiqicha", "qcc", "qichacha",
                    "zhihu", "sina", "163", "qq.com", "sohu", "toutiao", "douyin",
                    "weibo", "csdn", "oschina", "wikipedia"};
            while (m.find()) {
                String host = m.group(1).toLowerCase();
                boolean ok = true;
                for (String b : bad) if (host.contains(b)) { ok = false; break; }
                if (ok) return host;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** 搜索企业名列表（返回候选公司名）。 */
    public List<String> search(String q) {
        List<String> result = new ArrayList<>();
        try {
            q = q.trim();
            if (q.isEmpty()) return result;

            String cacheKey = "s:" + q;
            String cached = cache.getString(cacheKey, null);
            if (cached != null && !cached.isEmpty()) {
                for (String n : cached.split("\u0001")) if (!n.isEmpty()) result.add(n);
                if (!result.isEmpty()) return result;
            }

            String url = SEARCH + URLEncoder.encode(q + " 企业 工商信息 天眼查", "UTF-8");
            String html = getHtml(url);
            if (html != null) {
                Set<String> names = new LinkedHashSet<>();
                Matcher m = Pattern.compile("<h2>(.*?)</h2>", Pattern.DOTALL).matcher(html);
                while (m.find()) {
                    String t = cleanTitle(stripTags(m.group(1)));
                    if (looksLikeCompany(t)) names.add(t);
                }
                // 兜底：直接抽 b_algo 内的标题链接
                if (names.isEmpty()) {
                    Matcher m2 = Pattern.compile(
                            "class=\"b_algo\"[^>]*>.*?<a[^>]+href=\"[^\"]+\"[^>]*>(.*?)</a>",
                            Pattern.DOTALL).matcher(html);
                    while (m2.find()) {
                        String t = cleanTitle(stripTags(m2.group(1)));
                        if (looksLikeCompany(t)) names.add(t);
                    }
                }
                for (String n : names) {
                    if (result.size() >= 20) break;
                    result.add(n);
                }
                if (!result.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (String n : result) sb.append(n).append("\u0001");
                    cache.edit().putString(cacheKey, sb.toString()).apply();
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    /** 获取企业详情（信用代码/法人/资本/成立日期/状态/地址/股东/官网）。 */
    public Company getDetail(String name) {
        Company c = new Company(name);
        c.tip = defaultTip;
        try {
            String cacheKey = "d:" + name;
            String raw = cache.getString(cacheKey, null);
            if (raw != null && applyCached(c, raw)) {
                return c;
            }
            String url = SEARCH + URLEncoder.encode(
                    name + " 法定代表人 注册资本 成立日期 登记状态 注册地址 股东 官网", "UTF-8");
            String html = getHtml(url);
            if (html != null) {
                extract(html, c);
                if (c.website == null) c.website = pickWebsite(html);
                cache.edit().putString(cacheKey, serialize(c)).apply();
            }
        } catch (Exception ignored) {
        }
        return c;
    }

    private void extract(String html, Company c) {
        Matcher credit = Pattern.compile("([0-9A-Z]{18})").matcher(html);
        if (credit.find()) c.creditCode = credit.group(1);

        Matcher legal = Pattern.compile("法定代表人[：:]\\s*([\\u4e00-\\u9fa5·]{2,5})").matcher(html);
        if (legal.find()) c.legalPerson = legal.group(1);

        Matcher cap = Pattern.compile("注册资本[：:]\\s*([0-9.]+[万千万亿]?元?)").matcher(html);
        if (cap.find()) c.registeredCapital = cap.group(1);

        Matcher date = Pattern.compile("成立日期[：:]\\s*([0-9]{4}[-年][0-9]{1,2}[-月][0-9]{1,2}日?)").matcher(html);
        if (date.find()) c.establishDate = date.group(1);

        Matcher status = Pattern.compile("登记状态[：:]\\s*([\\u4e00-\\u9fa5]{2,6})").matcher(html);
        if (status.find()) c.status = status.group(1);

        Matcher addr = Pattern.compile("注册地址[：:]\\s*([\\u4e00-\\u9fa50-9a-zA-Z（）()号路街区栋室楼层.#\\-]{4,40})").matcher(html);
        if (addr.find()) c.regAddress = addr.group(1);

        Matcher sh = Pattern.compile("股东[：:：]?\\s*([\\u4e00-\\u9fa5·]{2,5}(?:、[\\u4e00-\\u9fa5·]{2,5}){0,5})").matcher(html);
        while (sh.find() && c.shareholders.size() < 10) c.shareholders.add(sh.group(1));
    }

    private String serialize(Company c) {
        StringBuilder sb = new StringBuilder();
        sb.append("name=").append(c.name).append("\n");
        if (c.creditCode != null) sb.append("creditCode=").append(c.creditCode).append("\n");
        if (c.legalPerson != null) sb.append("legalPerson=").append(c.legalPerson).append("\n");
        if (c.status != null) sb.append("status=").append(c.status).append("\n");
        if (c.registeredCapital != null) sb.append("registeredCapital=").append(c.registeredCapital).append("\n");
        if (c.establishDate != null) sb.append("establishDate=").append(c.establishDate).append("\n");
        if (c.regAddress != null) sb.append("regAddress=").append(c.regAddress).append("\n");
        if (c.website != null) sb.append("website=").append(c.website).append("\n");
        return sb.toString();
    }

    private boolean applyCached(Company c, String raw) {
        try {
            for (String line : raw.split("\n")) {
                int i = line.indexOf('=');
                if (i < 0) continue;
                String k = line.substring(0, i);
                String v = line.substring(i + 1);
                switch (k) {
                    case "creditCode": c.creditCode = v; break;
                    case "legalPerson": c.legalPerson = v; break;
                    case "status": c.status = v; break;
                    case "registeredCapital": c.registeredCapital = v; break;
                    case "establishDate": c.establishDate = v; break;
                    case "regAddress": c.regAddress = v; break;
                    case "website": c.website = v; break;
                    default: break;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static class Company {
        public final String name;
        public String creditCode, legalPerson, registeredCapital, establishDate, status, regAddress, website, tip;
        public final List<String> shareholders = new ArrayList<>();

        public Company(String name) {
            this.name = name;
        }
    }
}
