/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.os.StatFs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** 扫描各存储分区与内存区，还原“分区详情”界面所需数据。 */
public class PartitionScanner {

    // 文件系统分区挂载点（对应图中的 System / Data / Magisk=/data/adb / Cust 等）
    private static final String[][] FS_PARTITIONS = {
            {"System", "/system"},
            {"System Ext", "/system_ext"},
            {"Vendor", "/vendor"},
            {"Product", "/product"},
            {"ODM", "/odm"},
            {"Data", "/data"},
            {"Internal Storage", "/storage/emulated/0"},
            {"Cache", "/cache"},
            {"Cust", "/cust"},
            {"Persist", "/persist"},
            {"Magisk", "/data/adb"},
    };

    /** 用 StatFs 扫描可读的文件系统分区。 */
    public static List<PartitionInfo> scanFilesystem() {
        List<PartitionInfo> list = new ArrayList<>();
        for (String[] p : FS_PARTITIONS) {
            try {
                StatFs stat = new StatFs(p[1]);
                long total = stat.getTotalBytes();
                long free = stat.getFreeBytes();
                if (total <= 0) continue;
                PartitionInfo info = new PartitionInfo();
                info.name = p[0];
                info.mountPoint = p[1];
                info.total = total;
                info.free = free;
                info.type = PartitionInfo.TYPE_FILESYSTEM;
                list.add(info);
            } catch (Exception ignored) {
                // 该挂载点不存在或无权限，跳过
            }
        }
        return list;
    }

    /** 解析 /proc/meminfo 中某字段，返回字节数（文件内单位为 KB）。 */
    private static long readMemInfo(String key) {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(key)) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        return Long.parseLong(parts[1]) * 1024L;
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return 0;
    }

    /** 扫描内存类分区：RAM / Swap / Cached / Slab（数据来自 /proc/meminfo）。 */
    public static List<PartitionInfo> scanMemory() {
        List<PartitionInfo> list = new ArrayList<>();

        long memTotal = readMemInfo("MemTotal:");
        long memAvailable = readMemInfo("MemAvailable:");
        if (memAvailable == 0) {
            long memFree = readMemInfo("MemFree:");
            long buffers = readMemInfo("Buffers:");
            long cached = readMemInfo("Cached:");
            memAvailable = memFree + buffers + cached;
        }
        PartitionInfo ram = new PartitionInfo();
        ram.name = "RAM";
        ram.mountPoint = "memory";
        ram.total = memTotal;
        ram.free = memAvailable;
        ram.type = PartitionInfo.TYPE_MEMORY;
        list.add(ram);

        long swapTotal = readMemInfo("SwapTotal:");
        long swapFree = readMemInfo("SwapFree:");
        PartitionInfo swap = new PartitionInfo();
        swap.name = "Swap";
        swap.mountPoint = "swap";
        swap.total = swapTotal;
        swap.free = swapFree;
        swap.type = PartitionInfo.TYPE_MEMORY;
        list.add(swap);

        // Cached / Slab 为内核占用指标，以 MemTotal 作展示基线以呈现占比
        long cached = readMemInfo("Cached:");
        PartitionInfo cachedInfo = new PartitionInfo();
        cachedInfo.name = "Cached";
        cachedInfo.mountPoint = "cached";
        cachedInfo.total = memTotal;
        cachedInfo.free = Math.max(0, memTotal - cached);
        cachedInfo.type = PartitionInfo.TYPE_MEMORY;
        list.add(cachedInfo);

        long slab = readMemInfo("Slab:");
        PartitionInfo slabInfo = new PartitionInfo();
        slabInfo.name = "Slab";
        slabInfo.mountPoint = "slab";
        slabInfo.total = memTotal;
        slabInfo.free = Math.max(0, memTotal - slab);
        slabInfo.type = PartitionInfo.TYPE_MEMORY;
        list.add(slabInfo);

        return list;
    }
}
