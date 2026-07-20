package com.example.partitioncleaner;

/** 单个分区/内存区的展示模型。 */
public class PartitionInfo {
    public static final int TYPE_FILESYSTEM = 0;
    public static final int TYPE_MEMORY = 1;

    public String name;
    public String mountPoint;
    public long total;
    public long free;
    public int type;

    public long getUsed() {
        return Math.max(0, total - free);
    }

    public int getPercent() {
        if (total <= 0) return 0;
        return (int) (getUsed() * 100 / total);
    }
}
