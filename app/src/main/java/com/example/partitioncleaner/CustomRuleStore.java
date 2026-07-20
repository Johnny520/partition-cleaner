package com.example.partitioncleaner;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 用户自定义清理规则的持久化管理（SharedPreferences + JSON）。 */
public class CustomRuleStore {
    private static final String PREFS = "custom_rules";
    private static final String KEY = "rules";

    private final SharedPreferences sp;

    public CustomRuleStore(Context ctx) {
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public List<CustomRule> getAll() {
        List<CustomRule> list = new ArrayList<>();
        String raw = sp.getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(CustomRule.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
            // 数据损坏则忽略
        }
        return list;
    }

    public CustomRule getById(String id) {
        if (id == null) return null;
        for (CustomRule r : getAll()) {
            if (id.equals(r.id)) return r;
        }
        return null;
    }

    private void saveAll(List<CustomRule> list) {
        JSONArray arr = new JSONArray();
        try {
            for (CustomRule r : list) arr.put(r.toJson());
        } catch (JSONException ignored) {
        }
        sp.edit().putString(KEY, arr.toString()).apply();
    }

    public void add(CustomRule r) {
        List<CustomRule> list = getAll();
        list.add(r);
        saveAll(list);
    }

    public void update(CustomRule r) {
        List<CustomRule> list = getAll();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(r.id)) {
                list.set(i, r);
                break;
            }
        }
        saveAll(list);
    }

    public void remove(String id) {
        List<CustomRule> list = getAll();
        list.removeIf(r -> id != null && id.equals(r.id));
        saveAll(list);
    }
}
