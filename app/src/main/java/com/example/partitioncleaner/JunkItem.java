package com.example.partitioncleaner;

/** 一个待清理项（缓存/日志/临时文件/空文件夹/残留）。 */
public class JunkItem {
    public static final int TYPE_CACHE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_TEMP = 2;
    public static final int TYPE_EMPTY_DIR = 3;
    public static final int TYPE_RESIDUAL = 4;

    // 清理建议
    public static final int ADVICE_CLEAN = 0; // 建议清理（默认勾选）
    public static final int ADVICE_KEEP = 1;  // 建议保留（默认不勾选，有风险）

    public String path;
    public long size;
    public int type;
    public boolean selected = true;
    public int advice = ADVICE_CLEAN; // 默认建议清理
    public String reason = "";        // 建议原因，展示给用户

    public boolean isKeep() {
        return advice == ADVICE_KEEP;
    }

    public String adviceLabel() {
        return advice == ADVICE_KEEP ? "建议保留" : "建议清理";
    }

    public String typeLabel() {
        switch (type) {
            case TYPE_CACHE:
                return "缓存";
            case TYPE_LOG:
                return "日志";
            case TYPE_TEMP:
                return "临时文件";
            case TYPE_EMPTY_DIR:
                return "空文件夹";
            default:
                return "残留";
        }
    }
}
