package com.example.partitioncleaner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 扫描垃圾文件与空文件夹，并为每项给出清理建议（清理/保留）。 */
public class JunkScanner {

    // 普通应用可访问的用户区扫描根
    private static final String[] USER_SCAN_ROOTS = {
            "/storage/emulated/0/Android/data",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Tencent",
            "/storage/emulated/0/DCIM/.thumbnails",
    };

    // 需要 root 才能读取的系统/数据垃圾目录
    private static final String[] ROOT_SCAN_ROOTS = {
            "/cache",
            "/data/log",
            "/data/tombstones",
            "/data/anr",
            "/data/system/dropbox",
            "/data/local/tmp",
            "/system/cache",
            "/cust/log",
            "/data/data",
    };

    /** 扫描普通可访问目录下的缓存目录、日志、临时文件。 */
    public static List<JunkItem> scanUserJunk() {
        List<JunkItem> items = new ArrayList<>();
        for (String root : USER_SCAN_ROOTS) {
            scanDir(new File(root), items);
        }
        return items;
    }

    private static void scanDir(File dir, List<JunkItem> items) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return; // 无权限访问
        for (File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (name.equals("cache") || name.equals("Cache")) {
                    long s = sizeOf(f);
                    if (s > 0) {
                        JunkItem it = new JunkItem();
                        it.path = f.getAbsolutePath();
                        it.size = s;
                        it.type = JunkItem.TYPE_CACHE;
                        applyAdvice(it);
                        items.add(it);
                    }
                }
                scanDir(f, items);
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".log") || name.endsWith(".logcat")) {
                    addFile(f, items, JunkItem.TYPE_LOG);
                } else if (name.endsWith(".tmp") || name.endsWith(".temp") || name.endsWith(".bak")) {
                    addFile(f, items, JunkItem.TYPE_TEMP);
                }
            }
        }
    }

    private static void addFile(File f, List<JunkItem> items, int type) {
        JunkItem it = new JunkItem();
        it.path = f.getAbsolutePath();
        it.size = f.length();
        it.type = type;
        applyAdvice(it);
        items.add(it);
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

    /** 用 root 的 find 命令扫描系统分区的日志/临时文件。 */
    public static List<JunkItem> scanRootJunk(RootShell root) {
        List<JunkItem> items = new ArrayList<>();
        if (root == null || !root.isAvailable()) return items;

        StringBuilder cmd = new StringBuilder();
        for (String r : ROOT_SCAN_ROOTS) {
            cmd.append("find '").append(r).append("' -type f \\( -name '*.log' -o -name '*.logcat' ")
                    .append("-o -name '*.tmp' -o -name '*.temp' -o -name '*.bak' \\) ")
                    .append("-printf '%s\\t%p\\n' 2>/dev/null; ");
        }
        String out = root.run(cmd.toString());
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int idx = line.indexOf('\t');
            if (idx <= 0) continue;
            try {
                long s = Long.parseLong(line.substring(0, idx));
                String p = line.substring(idx + 1);
                JunkItem it = new JunkItem();
                it.path = p;
                it.size = s;
                String low = p.toLowerCase();
                it.type = (low.endsWith(".log") || low.endsWith(".logcat"))
                        ? JunkItem.TYPE_LOG : JunkItem.TYPE_TEMP;
                applyAdvice(it);
                items.add(it);
            } catch (Exception ignored) {
            }
        }
        return items;
    }

    /** 用 root 的 find 扫描给定根下的空文件夹。 */
    public static List<JunkItem> scanEmptyDirs(RootShell root, String[] roots) {
        List<JunkItem> items = new ArrayList<>();
        if (root == null || !root.isAvailable()) return items;
        StringBuilder cmd = new StringBuilder();
        for (String r : roots) {
            cmd.append("find '").append(r).append("' -type d -empty -printf '%p\\n' 2>/dev/null; ");
        }
        String out = root.run(cmd.toString());
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            JunkItem it = new JunkItem();
            it.path = line;
            it.size = 0;
            it.type = JunkItem.TYPE_EMPTY_DIR;
            applyAdvice(it);
            items.add(it);
        }
        return items;
    }

    // ===== 清理建议判定 =====

    /** 生成项后调用：根据路径与类型决定建议并设默认勾选。 */
    private static void applyAdvice(JunkItem it) {
        int a = decideAdvice(it.path, it.type);
        it.advice = a;
        it.reason = adviceReason(it.path, it.type, a);
        it.selected = (a == JunkItem.ADVICE_CLEAN); // 仅默认勾选“建议清理”
    }

    /** 根据路径与类型判断该项是建议清理还是建议保留。 */
    private static int decideAdvice(String path, int type) {
        String p = path.toLowerCase();
        // 系统只读分区：删除有变砖风险
        if (p.startsWith("/system") || p.startsWith("/system_ext") || p.startsWith("/vendor")
                || p.startsWith("/product") || p.startsWith("/odm") || p.startsWith("/cust")) {
            return JunkItem.ADVICE_KEEP;
        }
        // 应用私有数据目录：误删致应用崩溃
        if (p.startsWith("/data/data/") || p.startsWith("/data/user/")) {
            return JunkItem.ADVICE_KEEP;
        }
        // 非用户区的空文件夹：可能被程序依赖
        if (type == JunkItem.TYPE_EMPTY_DIR
                && !p.startsWith("/storage/emulated/0") && !p.startsWith("/cache")) {
            return JunkItem.ADVICE_KEEP;
        }
        return JunkItem.ADVICE_CLEAN;
    }

    private static String adviceReason(String path, int type, int advice) {
        if (advice == JunkItem.ADVICE_CLEAN) return "可安全删除";
        String p = path.toLowerCase();
        if (p.startsWith("/system") || p.startsWith("/system_ext") || p.startsWith("/vendor")
                || p.startsWith("/product") || p.startsWith("/odm") || p.startsWith("/cust")) {
            return "位于系统分区，删除可能导致系统异常或无法启动";
        }
        if (p.startsWith("/data/data/") || p.startsWith("/data/user/")) {
            return "应用私有数据目录，误删可能导致应用崩溃";
        }
        if (type == JunkItem.TYPE_EMPTY_DIR) {
            return "系统/数据区目录，可能被程序依赖，谨慎删除";
        }
        return "删除存在风险，建议保留";
    }
}
