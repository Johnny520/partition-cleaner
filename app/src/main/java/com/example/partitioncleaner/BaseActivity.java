package com.example.partitioncleaner;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** 所有 Activity 的基类：在创建界面前套用用户选择的主题，实现全局换肤。 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeManager.getThemeStyle(this));
        super.onCreate(savedInstanceState);
    }
}
