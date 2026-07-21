/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

/** 所有 Activity 的基类：在创建界面前套用用户选择的主题，实现全局换肤。 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getThemeStyle(this));
        super.onCreate(savedInstanceState);
    }

    /** 让页面根布局透出窗口的渐变背景，实现「整个应用渐变背景」。 */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        View content = findViewById(android.R.id.content);
        if (content == null) return;
        content.setBackgroundResource(android.R.color.transparent);
        if (content instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) content;
            if (vg.getChildCount() > 0) {
                vg.getChildAt(0).setBackgroundResource(android.R.color.transparent);
            }
        }
    }
}
