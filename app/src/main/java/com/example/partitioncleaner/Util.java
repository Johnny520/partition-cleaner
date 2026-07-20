/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

/** 通用工具方法。 */
public class Util {
    /** 把字节数格式化为人类可读字符串，如 "5.22 GB"。 */
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int i = (int) (Math.log(bytes) / Math.log(1024));
        if (i < 0) i = 0;
        if (i >= units.length) i = units.length - 1;
        double value = bytes / Math.pow(1024, i);
        return String.format("%.2f %s", value, units[i]);
    }
}
