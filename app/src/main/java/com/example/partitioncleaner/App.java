/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.app.Application;

/**
 * 应用入口：在一切之前注册全局崩溃捕获器，
 * 让任何未捕获异常都落盘到 分区清理大师/log，方便排查。
 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance().init(this);
    }
}
