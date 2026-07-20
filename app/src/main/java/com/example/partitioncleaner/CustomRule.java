/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * 用户自定义清理规则。可持久化到 SharedPreferences（JSON）。
 * mode: 0=按目录名匹配整目录 1=按文件后缀匹配 2=空文件。
 */
public class CustomRule {
    public static final int MODE_DIR = 0;
    public static final int MODE_FILE = 1;
    public static final int MODE_EMPTY = 2;

    public String id;
    public String name;
    public String[] roots;
    public int mode;
    public String[] patterns;
    public long minSize;   // 字节，0=不限
    public int advice;     // JunkItem.ADVICE_CLEAN / ADVICE_KEEP
    public String reason;

    public CustomRule() {
        this.id = java.util.UUID.randomUUID().toString();
        this.mode = MODE_DIR;
        this.advice = JunkItem.ADVICE_CLEAN;
        this.roots = new String[]{"/storage/emulated/0"};
        this.patterns = new String[]{};
    }

    public CustomRule(String name, String[] roots, int mode, String[] patterns,
                      long minSize, int advice, String reason) {
        this();
        this.name = name;
        this.roots = roots;
        this.mode = mode;
        this.patterns = patterns;
        this.minSize = minSize;
        this.advice = advice;
        this.reason = reason;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("name", name == null ? "" : name);
        o.put("mode", mode);
        o.put("minSize", minSize);
        o.put("advice", advice);
        o.put("reason", reason == null ? "" : reason);
        o.put("roots", new JSONArray(Arrays.asList(roots == null ? new String[0] : roots)));
        o.put("patterns", new JSONArray(Arrays.asList(patterns == null ? new String[0] : patterns)));
        return o;
    }

    public static CustomRule fromJson(JSONObject o) throws JSONException {
        CustomRule r = new CustomRule();
        r.id = o.optString("id", r.id);
        r.name = o.optString("name", "");
        r.mode = o.optInt("mode", MODE_DIR);
        r.minSize = o.optLong("minSize", 0);
        r.advice = o.optInt("advice", JunkItem.ADVICE_CLEAN);
        r.reason = o.optString("reason", "");
        r.roots = jsonToStringArray(o.optJSONArray("roots"));
        r.patterns = jsonToStringArray(o.optJSONArray("patterns"));
        return r;
    }

    private static String[] jsonToStringArray(JSONArray a) {
        if (a == null) return new String[0];
        String[] out = new String[a.length()];
        for (int i = 0; i < a.length(); i++) out[i] = a.optString(i, "");
        return out;
    }

    public String rootsText() {
        return roots == null ? "" : TextUtils.join(", ", roots);
    }

    public String modeText() {
        if (mode == MODE_DIR) return "目录名";
        if (mode == MODE_EMPTY) return "空文件";
        return "文件后缀";
    }

    public String patternsText() {
        if (mode == MODE_EMPTY) return "—";
        if (patterns == null || patterns.length == 0) return "全部";
        return TextUtils.join(", ", patterns);
    }

    public String adviceText() {
        return advice == JunkItem.ADVICE_KEEP ? "建议保留" : "建议清理";
    }
}
