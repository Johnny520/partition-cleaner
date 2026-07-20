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

    // ===== 扩展清理类型（社交 / 应用系统 / 文件类型） =====
    // 社交与内容平台
    public static final int TYPE_WECHAT = 12;
    public static final int TYPE_QQ = 13;
    public static final int TYPE_DOUYIN = 14;
    public static final int TYPE_BROWSER = 15;
    public static final int TYPE_SCREENSHOT = 16;
    public static final int TYPE_WEIBO = 17;
    public static final int TYPE_XHS = 18;
    public static final int TYPE_KUAISHOU = 19;
    public static final int TYPE_BILIBILI = 20;
    public static final int TYPE_WANGYI = 21;
    public static final int TYPE_TAOBAO = 22;
    public static final int TYPE_PDD = 23;
    // 应用与系统
    public static final int TYPE_APP_CACHE_ALL = 24;
    public static final int TYPE_SYSTEM_JUNK = 25;
    public static final int TYPE_MAPS = 26;
    public static final int TYPE_IME = 27;
    // 文件类型
    public static final int TYPE_MUSIC = 28;
    public static final int TYPE_VIDEO_FILE = 29;
    public static final int TYPE_DOC = 30;
    public static final int TYPE_ARCHIVE = 31;
    public static final int TYPE_DOWNLOAD = 32;
    public static final int TYPE_EMPTY_FILE = 33;
    public static final int TYPE_BLUETOOTH = 34;
    public static final int TYPE_RECORD = 35;
    public static final int TYPE_WALLPAPER = 36;

    // ===== v1.0.5 新增清理/聚合类型 =====
    public static final int TYPE_EBOOK = 37;       // 电子书扫描
    public static final int TYPE_APP_DATA = 38;    // 应用数据清理(聚合)
    public static final int TYPE_UNUSED_APP = 39;  // 长期未使用 APP(非扫描项)
    public static final int TYPE_DEEP = 40;        // 深度清理(聚合扫描)

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
            case TYPE_EBOOK:
                return "电子书";
            case TYPE_APP_DATA:
                return "应用数据";
            case TYPE_UNUSED_APP:
                return "长期未使用";
            case TYPE_DEEP:
                return "深度清理";
            default:
                return "残留";
        }
    }
}
