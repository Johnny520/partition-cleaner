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
 * 主路径：/storage/emulated/0/分区清理大师/log   （用户指定）
 * 保底路径：/storage/emulated/0/Android/data/<pkg>/files/分区清理大师/log （应用私有外部目录，无需授权一定能写）
 *
 * 若主路径因系统存储限制（Android 11+ 未授予“所有文件访问”权限、或未运行时授权）写不进，
 * 会自动降级到保底路径，保证崩溃日志不丢。
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
        boolean ok = tryWrite(primaryDir(), content);
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

    /** 尽力提示用户已复制（进程即将被系统终止，Toast 可能不显示，剪切板才是可靠来源） */
    private void showClipboardToast() {
        try {
            new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, "崩溃日志已自动复制到剪切板，去任意 App 长按粘贴发我", Toast.LENGTH_LONG).show());
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
}
