/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 扫描内部存储中内容相同的重复文件（先按大小分组，再对同大小文件算 MD5 确认）。 */
public class DuplicateScanner {

    // 扫描根：内部存储里常见会产生重复文件的目录
    private static final String[] DUP_ROOTS = {
            "/storage/emulated/0/DCIM",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Pictures",
            "/storage/emulated/0/Movies",
            "/storage/emulated/0/Documents",
            "/storage/emulated/0/Tencent",
            "/storage/emulated/0/Android/media",
            "/storage/emulated/0/WhatsApp",
    };

    // 超过该大小的文件跳过哈希（避免极慢），默认 300MB
    private static final long MAX_FILE_SIZE = 300L * 1024 * 1024;

    public static class DupGroup {
        public String hash;
        public long size;
        public List<String> paths = new ArrayList<>();
        public int keepIndex = 0; // 保留第几个（默认第一个）
    }

    private static class FileEntry {
        File file;
        String path;
        long size;
        String hash;
    }

    public static List<DupGroup> scan() {
        List<FileEntry> files = new ArrayList<>();
        for (String root : DUP_ROOTS) {
            collect(new File(root), files);
        }
        // 1) 按大小分组（大小不同的一定不重复）
        Map<Long, List<FileEntry>> bySize = new HashMap<>();
        for (FileEntry f : files) {
            bySize.computeIfAbsent(f.size, k -> new ArrayList<>()).add(f);
        }
        // 2) 同大小组内算 MD5，再按哈希归组
        List<DupGroup> groups = new ArrayList<>();
        for (List<FileEntry> sameSize : bySize.values()) {
            if (sameSize.size() < 2) continue;
            Map<String, List<FileEntry>> byHash = new HashMap<>();
            for (FileEntry f : sameSize) {
                f.hash = md5(f.file);
                if (f.hash == null) continue;
                byHash.computeIfAbsent(f.hash, k -> new ArrayList<>()).add(f);
            }
            for (List<FileEntry> sameHash : byHash.values()) {
                if (sameHash.size() < 2) continue;
                DupGroup g = new DupGroup();
                g.hash = sameHash.get(0).hash;
                g.size = sameHash.get(0).size;
                for (FileEntry f : sameHash) g.paths.add(f.path);
                groups.add(g);
            }
        }
        return groups;
    }

    private static void collect(File dir, List<FileEntry> out) {
        if (dir == null || !dir.isDirectory()) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) {
                collect(f, out);
            } else {
                long s = f.length();
                if (s <= 0 || s > MAX_FILE_SIZE) continue;
                FileEntry e = new FileEntry();
                e.file = f;
                e.path = f.getAbsolutePath();
                e.size = s;
                out.add(e);
            }
        }
    }

    private static String md5(File f) {
        try (InputStream in = new FileInputStream(f)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
            byte[] dig = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
