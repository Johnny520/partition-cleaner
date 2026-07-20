/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.widget.Toast;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全局未捕获异常处理器：把崩溃堆栈、设备/版本信息写到外部存储的日志文件。
 *
 * 主路径：/storage/emulated/0/分区清理大师/log   （用户指定，Android 11+ 可能无权限）
 * 公开路径：Download/分区清理大师_崩溃日志_*.txt  （MediaStore，文件管理器可直接访问，推荐取用）
 * 保底路径：/storage/emulated/0/Android/data/<pkg>/files/分区清理大师/log （应用私有外部目录，无需授权一定能写）
 *
 * 若主路径因系统存储限制（Android 11+ 未授予“所有文件访问”权限）写不进，
 * 会自动降级到公开 Download 目录 / 应用私有目录，保证崩溃日志一定拿得到。
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static final String DIR_NAME = "分区清理大师";
    private static final String LOG_SUBDIR = "log";

    private static CrashHandler INSTANCE;

    private Thread.UncaughtExceptionHandler defaultHandler;
    private Context context;

    public static CrashHandler getInstance() {
        if (INSTANCE == null) INSTANCE = new CrashHandler();
        return INSTANCE;
    }

    public void init(Context ctx) {
        context = ctx.getApplicationContext();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String report = null;
        try {
            report = writeCrashLog(thread, ex);
        } catch (Throwable t) {
            // 写日志本身失败不能影响后续流程
            android.util.Log.e(TAG, "写崩溃日志异常", t);
        }
        // 关键：自动把完整崩溃堆栈复制到剪切板，绕过 Android 11+ 隐藏 /Android/data/ 导致拿不到日志文件的问题
        if (report != null) copyToClipboard(report);
        showClipboardToast();
        // 交给系统默认处理器：弹出“已停止”对话框并终止进程
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
        }
    }

    private String writeCrashLog(Thread thread, Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append("时间: ").append(fmt(new Date())).append("\n");
        sb.append("应用: 分区清理大师 (").append(context.getPackageName()).append(")\n");
        sb.append("版本: ").append(getVersion()).append("\n");
        sb.append("设备: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        sb.append("系统: Android ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("线程: ").append(thread == null ? "?" : thread.getName()).append("\n");
        sb.append("------------------------------------\n");

        Writer w = new StringWriter();
        PrintWriter pw = new PrintWriter(w);
        ex.printStackTrace(pw);
        Throwable cause = ex.getCause();
        while (cause != null) {
            pw.append("\n[Caused by]\n");
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        pw.flush();
        sb.append(w.toString());
        pw.close();

        String content = sb.toString();
        // 依次尝试：用户指定目录 → 公开 Download（Android 11+ 文件管理器可直接访问）→ 应用私有目录（兜底）
        boolean ok = tryWrite(primaryDir(), content);
        if (!ok) ok = tryWritePublicDownloads(content);
        if (!ok) tryWrite(fallbackDir(), content);
        return content;
    }

    /** 把崩溃报告复制到系统剪切板：用户打开任意 App 长按粘贴即可发给开发者，无需去找日志文件 */
    private void copyToClipboard(String text) {
        try {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData cd = ClipData.newPlainText("分区清理大师崩溃日志", text);
                cm.setPrimaryClip(cd);
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "复制到剪切板失败", t);
        }
    }

    /** 尽力提示用户（进程即将被系统终止，Toast 可能不显示；Download 目录里的文件才是可靠来源） */
    private void showClipboardToast() {
        try {
            new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "崩溃日志已保存到 Download 文件夹（分区清理大师_崩溃日志_*.txt），去文件管理器发我", Toast.LENGTH_LONG).show());
        } catch (Throwable t) {
            android.util.Log.e(TAG, "提示 Toast 失败", t);
        }
    }

    private String getVersion() {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName + " (" + pi.versionCode + ")";
        } catch (Exception e) {
            try {
                return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
            } catch (Throwable t) {
                return "unknown";
            }
        }
    }

    public static File getLatestCrashLog(android.content.Context ctx) {
        File best = null;
        long bestTime = -1;
        File[] dirs = new File[2];
        dirs[0] = new File(Environment.getExternalStorageDirectory(),
                DIR_NAME + File.separator + LOG_SUBDIR);
        File ext = ctx.getExternalFilesDir(null);
        dirs[1] = ext == null ? null : new File(ext, DIR_NAME + File.separator + LOG_SUBDIR);
        for (File dir : dirs) {
            if (dir == null || !dir.exists()) continue;
            String[] names = dir.list();
            if (names == null) continue;
            for (String name : names) {
                if (name.startsWith("crash_") && name.endsWith(".txt")) {
                    File f = new File(dir, name);
                    if (f.lastModified() > bestTime) { bestTime = f.lastModified(); best = f; }
                }
            }
        }
        return best;
    }

    private File primaryDir() {
        return new File(Environment.getExternalStorageDirectory(),
                DIR_NAME + File.separator + LOG_SUBDIR);
    }

    private File fallbackDir() {
        File ext = context.getExternalFilesDir(null);
        if (ext == null) ext = context.getFilesDir();
        return new File(ext, DIR_NAME + File.separator + LOG_SUBDIR);
    }

    private boolean tryWrite(File dir, String content) {
        if (dir == null) return false;
        if (!dir.exists() && !dir.mkdirs()) return false;
        String name = "crash_" + fmtFileName(new Date()) + ".txt";
        File f = new File(dir, name);
        try (FileWriter fw = new FileWriter(f)) {
            fw.write(content);
            return true;
        } catch (Exception e) {
            android.util.Log.e(TAG, "写入失败: " + f.getAbsolutePath(), e);
            return false;
        }
    }

    private String fmt(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(d);
    }

    private String fmtFileName(Date d) {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(d);
    }

    /**
     * 写到公开的 Download 目录：Android 11+ 上 /Android/data/ 被系统隐藏，用户拿不到日志；
     * 用 MediaStore 写入 Downloads 后，文件管理器里直接可见，用户长按分享/发送即可。
     */
    private boolean tryWritePublicDownloads(String content) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentResolver cr = context.getContentResolver();
                ContentValues cv = new ContentValues();
                String name = "分区清理大师_崩溃日志_" + fmtFileName(new Date()) + ".txt";
                cv.put(MediaStore.Downloads.DISPLAY_NAME, name);
                cv.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                cv.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                if (uri == null) return false;
                try (OutputStream os = cr.openOutputStream(uri)) {
                    if (os == null) return false;
                    os.write(content.getBytes(StandardCharsets.UTF_8));
                    return true;
                }
            } else {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists() && !dir.mkdirs()) return false;
                String name = "分区清理大师_崩溃日志_" + fmtFileName(new Date()) + ".txt";
                File f = new File(dir, name);
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write(content);
                    return true;
                }
            }
        } catch (Throwable t) {
            android.util.Log.e(TAG, "写入 Download 目录失败", t);
            return false;
        }
    }
}
