package com.example.partitioncleaner;

/** 一个待清理项（缓存/日志/临时文件/空文件夹/残留）。 */
public class JunkItem {
    public static final int TYPE_CACHE = 0;
    public static final int TYPE_LOG = 1;
    public static final int TYPE_TEMP = 2;
    public static final int TYPE_EMPTY_DIR = 3;
    public static final int TYPE_RESIDUAL = 4;
    public static final int TYPE_APP_CACHE = 5;  // 社交/视频 App 缓存目录
    public static final int TYPE_PHOTO = 6;      // 图片（微信图片/截图）
    public static final int TYPE_VIDEO = 7;      // 视频
    public static final int TYPE_APK = 8;        // 安装包
    public static final int TYPE_LARGE = 9;      // 大文件
    public static final int TYPE_THUMB = 10;     // 缩略图
    public static final int TYPE_AD = 11;        // 广告残留

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
            case TYPE_APP_CACHE:
                return "应用缓存";
            case TYPE_PHOTO:
                return "图片";
            case TYPE_VIDEO:
                return "视频";
            case TYPE_APK:
                return "安装包";
            case TYPE_LARGE:
                return "大文件";
            case TYPE_THUMB:
                return "缩略图";
            case TYPE_AD:
                return "广告残留";
            default:
                return "残留";
        }
    }
}
