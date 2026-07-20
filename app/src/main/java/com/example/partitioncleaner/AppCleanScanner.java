package com.example.partitioncleaner;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 市面上常见的 App 数据与文件清理扫描引擎。
 * 覆盖：微信 / QQ / 抖音 / 浏览器缓存、安装包、截图、日志、临时文件、
 * 大文件、卸载残留、缩略图、广告残留。
 *
 * 扫描优先使用 root 的 find/du（可访问 /Android/data 等受限制目录）；
 * 无 root 时降级为 Java 文件遍历（仅能访问未被限制的区域）。
 */
public class AppCleanScanner {

    // ===== 清理类型 =====
    public static final int TYPE_WECHAT = 1;     // 微信清理
    public static final int TYPE_QQ = 2;         // QQ 清理
    public static final int TYPE_DOUYIN = 3;      // 抖音清理
    public static final int TYPE_BROWSER = 4;     // 浏览器清理
    public static final int TYPE_APK = 5;         // 安装包清理
    public static final int TYPE_SCREENSHOT = 6;  // 截图清理
    public static final int TYPE_LOG = 7;         // 日志清理
    public static final int TYPE_TEMP = 8;        // 临时文件清理
    public static final int TYPE_LARGE = 9;       // 大文件清理
    public static final int TYPE_RESIDUAL = 10;   // 卸载残留
    public static final int TYPE_THUMB = 11;      // 缩略图清理
    public static final int TYPE_AD = 12;         // 广告残留清理

    private static final long MB = 1024L * 1024L;
    private static final int MAX_DEPTH = 10;

    /** 该类型对应的界面标题字符串资源 id。 */
    public static int getTitleRes(int type) {
        switch (type) {
            case TYPE_WECHAT: return R.string.clean_wechat;
            case TYPE_QQ: return R.string.clean_qq;
            case TYPE_DOUYIN: return R.string.clean_douyin;
            case TYPE_BROWSER: return R.string.clean_browser;
            case TYPE_APK: return R.string.clean_apk;
            case TYPE_SCREENSHOT: return R.string.clean_screenshot;
            case TYPE_LOG: return R.string.clean_log;
            case TYPE_TEMP: return R.string.clean_temp;
            case TYPE_LARGE: return R.string.clean_large;
            case TYPE_RESIDUAL: return R.string.clean_residual;
            case TYPE_THUMB: return R.string.clean_thumb;
            case TYPE_AD: return R.string.clean_ad;
            default: return R.string.cat_clean;
        }
    }

    // 扫描模式
    private static final int MODE_DIR = 0;   // 按目录名匹配整目录
    private static final int MODE_FILE = 1;  // 按文件扩展名匹配

    /** 一种清理类型的扫描规格。 */
    private static class Spec {
        String[] roots;
        int mode;
        String[] patterns;   // MODE_DIR: 目录名; MODE_FILE: 扩展名（含点，小写）
        long minSize;        // MODE_FILE 下最小字节，0=不限（匹配所有文件）
        int junkType;
        int advice;
        String reason;
    }

    private static Spec getSpec(int type) {
        Spec s = new Spec();
        s.minSize = 0;
        s.advice = JunkItem.ADVICE_CLEAN;
        switch (type) {
            case TYPE_WECHAT:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.tencent.mm",
                        "/storage/emulated/0/Tencent/MicroMsg"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"image2", "video", "voice2", "emoji",
                        "cache", "thumb", "tmp", "WeChat", "favorite"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "微信聊天产生的图片/视频/语音/表情缓存，清理后需重新加载";
                break;
            case TYPE_QQ:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.tencent.mobileqq",
                        "/storage/emulated/0/Tencent/QQfile_recv",
                        "/storage/emulated/0/Tencent/QQ_Images",
                        "/storage/emulated/0/Tencent/MobileQQ"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "thumb", "image", "ptt",
                        "chatpic", "nc_dvideo", "tmp", "qfile"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "QQ 聊天图片/语音/缓存文件，可安全清理";
                break;
            case TYPE_DOUYIN:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.ss.android.ugc.aweme",
                        "/storage/emulated/0/Android/data/com.ss.android.ugc.aweme.lite"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "video", "tmp", "ad", "adcache"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "抖音缓存的视频与临时文件，清理不影响已发布内容";
                break;
            case TYPE_BROWSER:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.android.chrome",
                        "/storage/emulated/0/Android/data/com.UCMobile",
                        "/storage/emulated/0/Android/data/com.tencent.mtt",
                        "/storage/emulated/0/Android/data/com.quark.browser",
                        "/storage/emulated/0/Android/data/com.baidu.searchbox",
                        "/storage/emulated/0/Android/data/com.ucmobile"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "thumbnails", ".thumbnails", "ad", "ads"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "浏览器缓存与缩略图，可安全清理";
                break;
            case TYPE_APK:
                s.roots = new String[]{
                        "/storage/emulated/0/Download",
                        "/storage/emulated/0/Android/data",
                        "/storage/emulated/0"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".apk"};
                s.junkType = JunkItem.TYPE_APK;
                s.reason = "已下载的安装包，确认不再需要时删除可释放空间";
                break;
            case TYPE_SCREENSHOT:
                s.roots = new String[]{
                        "/storage/emulated/0/DCIM/Screenshots",
                        "/storage/emulated/0/Pictures/Screenshots"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".png", ".jpg", ".jpeg", ".webp"};
                s.junkType = JunkItem.TYPE_PHOTO;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "截图文件，删除后不可恢复，请手动确认";
                break;
            case TYPE_LOG:
                s.roots = new String[]{
                        "/storage/emulated/0",
                        "/storage/emulated/0/Android/data",
                        "/storage/emulated/0/log",
                        "/storage/emulated/0/Android/obb"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".log", ".logcat", ".logcat.txt", ".trace"};
                s.junkType = JunkItem.TYPE_LOG;
                s.reason = "应用日志与崩溃记录，可安全清理";
                break;
            case TYPE_TEMP:
                s.roots = new String[]{
                        "/storage/emulated/0",
                        "/storage/emulated/0/Android/data",
                        "/storage/emulated/0/Download"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".tmp", ".temp", ".bak", ".part",
                        ".crdownload", ".download"};
                s.junkType = JunkItem.TYPE_TEMP;
                s.reason = "下载/编辑产生的临时文件，可安全清理";
                break;
            case TYPE_LARGE:
                s.roots = new String[]{
                        "/storage/emulated/0",
                        "/storage/emulated/0/Download",
                        "/storage/emulated/0/Movies",
                        "/storage/emulated/0/DCIM",
                        "/storage/emulated/0/Android/data"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{}; // 所有文件
                s.minSize = 100 * MB;
                s.junkType = JunkItem.TYPE_LARGE;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "大于 100MB 的大文件，请确认是否可删";
                break;
            case TYPE_THUMB:
                s.roots = new String[]{
                        "/storage/emulated/0/DCIM/.thumbnails",
                        "/storage/emulated/0/Pictures/.thumbnails",
                        "/storage/emulated/0/Android/data"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{".thumbnails", "thumbnails", ".thumbs", "thumbs"};
                s.junkType = JunkItem.TYPE_THUMB;
                s.reason = "系统缩略图缓存，清理后自动重建，可安全清理";
                break;
            case TYPE_AD:
                s.roots = new String[]{"/storage/emulated/0/Android/data"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"ad", "ads", ".ad", "adcache", "ad_union"};
                s.junkType = JunkItem.TYPE_AD;
                s.reason = "应用广告缓存目录，可安全清理";
                break;
            default:
                return null;
        }
        return s;
    }

    /** 扫描指定类型的可清理项。 */
    public static List<JunkItem> scan(Context ctx, int type, RootShell root) {
        if (type == TYPE_RESIDUAL) return scanResidual(ctx, root);
        Spec s = getSpec(type);
        if (s == null) return new ArrayList<>();
        List<JunkItem> items = new ArrayList<>();
        if (root != null && root.isAvailable()) {
            scanWithRoot(s, root, items);
        } else {
            scanWithFile(s, items);
        }
        return items;
    }

    /* ===================== root 扫描 ===================== */
    private static void scanWithRoot(Spec s, RootShell root, List<JunkItem> items) {
        if (s.mode == MODE_DIR) {
            StringBuilder find = new StringBuilder("find");
            for (String r : s.roots) find.append(" '").append(r).append("'");
            find.append(" -type d \\( ");
            for (int i = 0; i < s.patterns.length; i++) {
                if (i > 0) find.append(" -o ");
                find.append("-name '").append(s.patterns[i]).append("'");
            }
            find.append(" \\) -printf '%p\\n' 2>/dev/null");
            String dirs = root.run(find.toString());
            if (dirs.isEmpty()) return;
            StringBuilder calc = new StringBuilder();
            calc.append("printf '%s' \"").append(dirs.replace("\"", "'")).append("\"")
                    .append(" | while read -r d; do sz=$(du -sb \"$d\" 2>/dev/null | cut -f1);")
                    .append(" echo \"${sz:-0}\\t$d\"; done");
            parseLines(root.run(calc.toString()), s, items);
        } else {
            StringBuilder find = new StringBuilder("find");
            for (String r : s.roots) find.append(" '").append(r).append("'");
            if (s.patterns.length > 0) {
                find.append(" -type f \\( ");
                for (int i = 0; i < s.patterns.length; i++) {
                    if (i > 0) find.append(" -o ");
                    find.append("-name '*").append(s.patterns[i]).append("'");
                }
                find.append(" \\)");
            } else {
                find.append(" -type f");
            }
            find.append(" -printf '%s\\t%p\\n' 2>/dev/null");
            parseLines(root.run(find.toString()), s, items);
        }
    }

    private static void parseLines(String out, Spec s, List<JunkItem> items) {
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int idx = line.indexOf('\t');
            if (idx <= 0) continue;
            long sz;
            try {
                sz = Long.parseLong(line.substring(0, idx).trim());
            } catch (Exception e) {
                sz = 0;
            }
            if (s.minSize > 0 && sz < s.minSize) continue;
            JunkItem it = new JunkItem();
            it.size = sz;
            it.path = line.substring(idx + 1).trim();
            it.type = s.junkType;
            it.advice = s.advice;
            it.reason = s.reason;
            items.add(it);
        }
    }

    /* ===================== 无 root 降级扫描 ===================== */
    private static void scanWithFile(Spec s, List<JunkItem> items) {
        Set<File> roots = new HashSet<>();
        for (String r : s.roots) roots.add(new File(r));
        for (File rootDir : roots) {
            if (rootDir == null || !rootDir.isDirectory()) continue;
            if (s.mode == MODE_DIR) collectDirs(rootDir, s, items, 0);
            else collectFiles(rootDir, s, items, 0);
        }
    }

    private static void collectDirs(File dir, Spec s, List<JunkItem> out, int depth) {
        if (depth > MAX_DEPTH || dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (!f.isDirectory()) continue;
            String name = f.getName();
            for (String p : s.patterns) {
                if (name.equals(p)) {
                    JunkItem it = new JunkItem();
                    it.path = f.getAbsolutePath();
                    it.size = sizeOf(f);
                    it.type = s.junkType;
                    it.advice = s.advice;
                    it.reason = s.reason;
                    out.add(it);
                    break;
                }
            }
            collectDirs(f, s, out, depth + 1);
        }
    }

    private static void collectFiles(File dir, Spec s, List<JunkItem> out, int depth) {
        if (depth > MAX_DEPTH || dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectFiles(f, s, out, depth + 1);
            } else {
                String name = f.getName().toLowerCase();
                boolean match = s.patterns.length == 0;
                if (!match) {
                    for (String p : s.patterns) {
                        if (name.endsWith(p)) { match = true; break; }
                    }
                }
                if (match) {
                    long sz = f.length();
                    if (s.minSize > 0 && sz < s.minSize) continue;
                    JunkItem it = new JunkItem();
                    it.path = f.getAbsolutePath();
                    it.size = sz;
                    it.type = s.junkType;
                    it.advice = s.advice;
                    it.reason = s.reason;
                    out.add(it);
                }
            }
        }
    }

    private static long sizeOf(File dir) {
        long total = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) total += sizeOf(f);
            else total += f.length();
        }
        return total;
    }

    /* ===================== 卸载残留 ===================== */
    private static List<JunkItem> scanResidual(Context ctx, RootShell root) {
        List<JunkItem> items = new ArrayList<>();
        String[] bases = {
                "/storage/emulated/0/Android/data",
                "/storage/emulated/0/Android/obb",
                "/storage/emulated/0/Android/media"};
        PackageManager pm = ctx != null ? ctx.getPackageManager() : null;
        for (String base : bases) {
            File dir = new File(base);
            if (root != null && root.isAvailable()) {
                String out = root.run("ls -1 '" + base + "' 2>/dev/null");
                if (!out.isEmpty()) {
                    for (String name : out.split("\n")) {
                        name = name.trim();
                        if (name.isEmpty()) continue;
                        if (pm != null && isInstalled(pm, name)) continue;
                        JunkItem it = new JunkItem();
                        it.path = base + "/" + name;
                        it.size = duSize(root, it.path);
                        it.type = JunkItem.TYPE_RESIDUAL;
                        it.advice = JunkItem.ADVICE_KEEP;
                        it.reason = "已卸载应用 " + name + " 的残留目录";
                        items.add(it);
                    }
                    continue;
                }
            }
            File[] subs = dir.listFiles();
            if (subs == null) continue;
            for (File f : subs) {
                if (!f.isDirectory()) continue;
                String name = f.getName();
                if (pm != null && isInstalled(pm, name)) continue;
                JunkItem it = new JunkItem();
                it.path = f.getAbsolutePath();
                it.size = sizeOf(f);
                it.type = JunkItem.TYPE_RESIDUAL;
                it.advice = JunkItem.ADVICE_KEEP;
                it.reason = "已卸载应用 " + name + " 的残留目录";
                items.add(it);
            }
        }
        return items;
    }

    private static long duSize(RootShell root, String path) {
        String s = root.run("du -sb '" + path + "' 2>/dev/null | cut -f1");
        s = s.trim();
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private static boolean isInstalled(PackageManager pm, String pkg) {
        try {
            pm.getPackageInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
