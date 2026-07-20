/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.content.Context;
import android.content.SharedPreferences;

/** 全局主题管理：翡翠绿 / 深海蓝 / 魅影紫 / 炽焰红 / 曜金黑 / 液态玻璃（默认）。 */
public final class ThemeManager {

    public static final int THEME_EMERALD = 0;
    public static final int THEME_BLUE = 1;
    public static final int THEME_PURPLE = 2;
    public static final int THEME_RED = 3;
    public static final int THEME_DARKGOLD = 4;
    public static final int THEME_LIQUIDGLASS = 5;

    private static final String PREF = "app_prefs";
    private static final String KEY = "theme";

    public static final String[] NAMES = {"翡翠绿", "深海蓝", "魅影紫", "炽焰红", "曜金黑", "液态玻璃"};
    public static final String[] DESCS = {
            "清新自然 · 经典绿",
            "沉稳深邃 · 治愈系",
            "神秘优雅 · 浪漫调",
            "热情张扬 · 醒目红",
            "低调奢华 · 暗夜金",
            "通透磨砂 · 默认之选"
    };
    // 预览色（主色），用于主题卡片色块
    public static final int[] PREVIEW = {
            0xFF006A60, 0xFF1565C0, 0xFF6750A4, 0xFFBA1A1A, 0xFFE6C66E, 0xFF4F7CFF
    };

    public static int getTheme(Context c) {
        SharedPreferences sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        return sp.getInt(KEY, THEME_LIQUIDGLASS);
    }

    public static void setTheme(Context c, int t) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY, t).apply();
    }

    public static int getThemeStyle(Context c) {
        return getThemeStyleRes(getTheme(c));
    }

    public static int getThemeStyleRes(int t) {
        switch (t) {
            case THEME_BLUE:     return R.style.Theme_PartitionCleaner_Blue;
            case THEME_PURPLE:   return R.style.Theme_PartitionCleaner_Purple;
            case THEME_RED:      return R.style.Theme_PartitionCleaner_Red;
            case THEME_DARKGOLD: return R.style.Theme_PartitionCleaner_DarkGold;
            case THEME_LIQUIDGLASS: return R.style.Theme_LiquidGlass;
            default:             return R.style.Theme_PartitionCleaner;
        }
    }

    private ThemeManager() {}
}
