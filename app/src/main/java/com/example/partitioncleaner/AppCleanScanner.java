package com.example.partitioncleaner;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 市面上常见的 App 数据与文件清理扫描引擎（数据驱动 + 用户自定义）。
 * 内置覆盖：微信 / QQ / 抖音 / 浏览器缓存、安装包、截图、日志、临时文件、
 * 大文件、卸载残留、缩略图、广告残留；以及微博 / 小红书 / 快手 / B站 / 网易云 /
 * 淘宝 / 拼多多等社交内容平台；应用缓存、系统垃圾、地图离线包、输入法缓存；
 * 音乐 / 视频 / 文档 / 压缩包 / 下载 / 蓝牙 / 录音 / 壁纸 / 空文件等。
 * 同时支持 scanCustom() 扫描用户自定义规则（CustomRule）。
 *
 * 扫描优先使用 root 的 find/du（可访问受限目录）；无 root 时降级为 Java 遍历。
 */
public class AppCleanScanner {

    // ===== 清理类型常量（别名指向 JunkItem，确保全局唯一值与标题一致） =====
    public static final int TYPE_WECHAT = JunkItem.TYPE_WECHAT;
    public static final int TYPE_QQ = JunkItem.TYPE_QQ;
    public static final int TYPE_DOUYIN = JunkItem.TYPE_DOUYIN;
    public static final int TYPE_BROWSER = JunkItem.TYPE_BROWSER;
    public static final int TYPE_APK = JunkItem.TYPE_APK;
    public static final int TYPE_SCREENSHOT = JunkItem.TYPE_SCREENSHOT;
    public static final int TYPE_LOG = JunkItem.TYPE_LOG;
    public static final int TYPE_TEMP = JunkItem.TYPE_TEMP;
    public static final int TYPE_LARGE = JunkItem.TYPE_LARGE;
    public static final int TYPE_RESIDUAL = JunkItem.TYPE_RESIDUAL;
    public static final int TYPE_THUMB = JunkItem.TYPE_THUMB;
    public static final int TYPE_AD = JunkItem.TYPE_AD;
    public static final int TYPE_WEIBO = JunkItem.TYPE_WEIBO;
    public static final int TYPE_XHS = JunkItem.TYPE_XHS;
    public static final int TYPE_KUAISHOU = JunkItem.TYPE_KUAISHOU;
    public static final int TYPE_BILIBILI = JunkItem.TYPE_BILIBILI;
    public static final int TYPE_WANGYI = JunkItem.TYPE_WANGYI;
    public static final int TYPE_TAOBAO = JunkItem.TYPE_TAOBAO;
    public static final int TYPE_PDD = JunkItem.TYPE_PDD;
    public static final int TYPE_APP_CACHE = JunkItem.TYPE_APP_CACHE;
    public static final int TYPE_SYSTEM_JUNK = JunkItem.TYPE_SYSTEM_JUNK;
    public static final int TYPE_MAPS = JunkItem.TYPE_MAPS;
    public static final int TYPE_IME = JunkItem.TYPE_IME;
    public static final int TYPE_MUSIC = JunkItem.TYPE_MUSIC;
    public static final int TYPE_VIDEO_FILE = JunkItem.TYPE_VIDEO_FILE;
    public static final int TYPE_DOC = JunkItem.TYPE_DOC;
    public static final int TYPE_ARCHIVE = JunkItem.TYPE_ARCHIVE;
    public static final int TYPE_DOWNLOAD = JunkItem.TYPE_DOWNLOAD;
    public static final int TYPE_EMPTY_FILE = JunkItem.TYPE_EMPTY_FILE;
    public static final int TYPE_BLUETOOTH = JunkItem.TYPE_BLUETOOTH;
    public static final int TYPE_RECORD = JunkItem.TYPE_RECORD;
    public static final int TYPE_WALLPAPER = JunkItem.TYPE_WALLPAPER;

    // ===== v1.0.5 新增 =====
    public static final int TYPE_PHOTO = JunkItem.TYPE_PHOTO;
    public static final int TYPE_EBOOK = JunkItem.TYPE_EBOOK;
    public static final int TYPE_APP_DATA = JunkItem.TYPE_APP_DATA;
    public static final int TYPE_UNUSED_APP = JunkItem.TYPE_UNUSED_APP;
    public static final int TYPE_DEEP = JunkItem.TYPE_DEEP;

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
            case TYPE_WEIBO: return R.string.clean_weibo;
            case TYPE_XHS: return R.string.clean_xhs;
            case TYPE_KUAISHOU: return R.string.clean_kuaishou;
            case TYPE_BILIBILI: return R.string.clean_bilibili;
            case TYPE_WANGYI: return R.string.clean_wangyi;
            case TYPE_TAOBAO: return R.string.clean_taobao;
            case TYPE_PDD: return R.string.clean_pdd;
            case TYPE_APP_CACHE: return R.string.clean_appcache;
            case TYPE_SYSTEM_JUNK: return R.string.clean_systemjunk;
            case TYPE_MAPS: return R.string.clean_maps;
            case TYPE_IME: return R.string.clean_ime;
            case TYPE_MUSIC: return R.string.clean_music;
            case TYPE_VIDEO_FILE: return R.string.clean_videofile;
            case TYPE_DOC: return R.string.clean_doc;
            case TYPE_ARCHIVE: return R.string.clean_archive;
            case TYPE_DOWNLOAD: return R.string.clean_download;
            case TYPE_EMPTY_FILE: return R.string.clean_emptyfile;
            case TYPE_BLUETOOTH: return R.string.clean_bluetooth;
            case TYPE_RECORD: return R.string.clean_record;
            case TYPE_WALLPAPER: return R.string.clean_wallpaper;
            case TYPE_PHOTO: return R.string.clean_photo;
            case TYPE_EBOOK: return R.string.clean_ebook;
            case TYPE_APP_DATA: return R.string.clean_app_data;
            case TYPE_UNUSED_APP: return R.string.clean_unused;
            case TYPE_DEEP: return R.string.clean_deep;
            default: return R.string.cat_clean;
        }
    }

    // 扫描模式
    private static final int MODE_DIR = 0;   // 按目录名匹配整目录
    private static final int MODE_FILE = 1;  // 按文件扩展名匹配
    private static final int MODE_EMPTY = 2; // 空文件（0 字节）

    /** 一种清理类型的扫描规格。 */
    private static class Spec {
        String[] roots;
        int mode;
        String[] patterns;   // MODE_DIR: 目录名; MODE_FILE: 扩展名（含点，小写）
        long minSize;        // MODE_FILE 下最小字节，0=不限
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
            // ===== 社交 / 内容平台 =====
            case TYPE_WEIBO:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.sina.weibo",
                        "/storage/emulated/0/Android/data/com.sina.weibolite",
                        "/storage/emulated/0/sina/weibo"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "image", "video", "tmp", "stickers", "meipai", "webview"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "微博缓存的图片/视频/临时文件，可安全清理";
                break;
            case TYPE_XHS:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.xingin.xhs",
                        "/storage/emulated/0/Android/data/com.xhs.partner"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "image", "video", "tmp", "download", "webview"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "小红书缓存的图片/视频，可安全清理";
                break;
            case TYPE_KUAISHOU:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.smile.gifmaker",
                        "/storage/emulated/0/Android/data/com.kuaishou.nebula"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "video", "tmp", "gif", "ad"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "快手缓存的视频/临时文件，可安全清理";
                break;
            case TYPE_BILIBILI:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/tv.danmaku.bili",
                        "/storage/emulated/0/Android/data/com.bilibili.studio"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "download", "tmp", "danmaku", "vcache", "webview"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "B站缓存的视频/弹幕/临时文件，可安全清理";
                break;
            case TYPE_WANGYI:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.netease.cloudmusic",
                        "/storage/emulated/0/netease/cloudmusic"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "Album", "lyrics", "tmp", "download", "webview"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "网易云音乐缓存与歌词缓存，清理后需重新加载";
                break;
            case TYPE_TAOBAO:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.taobao.taobao",
                        "/storage/emulated/0/Android/data/com.taobao.idlefish",
                        "/storage/emulated/0/Android/data/com.tmall.wireless"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "image", "tmp", "news", "view", "webview", "ad"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "淘宝/闲鱼缓存图片与网页缓存，可安全清理";
                break;
            case TYPE_PDD:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.xunmeng.pinduoduo"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "image", "tmp", "webview", "ad", "download"};
                s.junkType = JunkItem.TYPE_APP_CACHE;
                s.reason = "拼多多缓存图片与网页缓存，可安全清理";
                break;
            // ===== 应用 / 系统 =====
            case TYPE_APP_CACHE:
                s.roots = new String[]{"/storage/emulated/0/Android/data"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "caches", "tmp", "temp"};
                s.junkType = JunkItem.TYPE_APP_CACHE_ALL;
                s.reason = "各应用缓存目录（需 root 才能访问大部分），清理后需重新加载";
                break;
            case TYPE_SYSTEM_JUNK:
                s.roots = new String[]{
                        "/data/anr",
                        "/data/tombstones",
                        "/data/system/dropbox",
                        "/cache",
                        "/data/log",
                        "/data/logs",
                        "/storage/emulated/0/log"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".log", ".txt", ".trace", ".dump", ".bugreport"};
                s.junkType = JunkItem.TYPE_SYSTEM_JUNK;
                s.reason = "系统崩溃/ANR/调试日志，需 root，可安全清理";
                break;
            case TYPE_MAPS:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.autonavi.minimap",
                        "/storage/emulated/0/Android/data/com.baidu.BaiduMap",
                        "/storage/emulated/0/Android/data/com.tencent.map",
                        "/storage/emulated/0/Android/data/com.google.android.apps.maps"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"offline", "mapdata", "vmp", "offlineMap", "navi", "cache", "maps"};
                s.junkType = JunkItem.TYPE_APP_CACHE_ALL;
                s.reason = "地图离线包与缓存，确认不需要离线导航时可清理";
                break;
            case TYPE_IME:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.sohu.inputmethod.sogou",
                        "/storage/emulated/0/Android/data/com.iflytek.inputmethod",
                        "/storage/emulated/0/Android/data/com.baidu.input",
                        "/storage/emulated/0/Android/data/com.cootek.smartinputv5",
                        "/storage/emulated/0/Android/data/com.touchtype.swiftkey"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "tmp", "theme", "skin", "dict", "webview"};
                s.junkType = JunkItem.TYPE_IME;
                s.reason = "输入法缓存与词库缓存，清理后可能需重新下载词库";
                break;
            // ===== 文件类型 =====
            case TYPE_MUSIC:
                s.roots = new String[]{
                        "/storage/emulated/0/Music",
                        "/storage/emulated/0",
                        "/storage/emulated/0/DCIM"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".mp3", ".flac", ".wav", ".ogg", ".aac", ".ape"};
                s.junkType = JunkItem.TYPE_MUSIC;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "音乐/音频文件，请确认是否可删";
                break;
            case TYPE_VIDEO_FILE:
                s.roots = new String[]{
                        "/storage/emulated/0/Movies",
                        "/storage/emulated/0/DCIM",
                        "/storage/emulated/0/Download",
                        "/storage/emulated/0"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".mp4", ".mkv", ".avi", ".mov", ".3gp", ".webm", ".flv"};
                s.junkType = JunkItem.TYPE_VIDEO_FILE;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "视频文件，请确认是否可删";
                break;
            case TYPE_DOC:
                s.roots = new String[]{
                        "/storage/emulated/0/Documents",
                        "/storage/emulated/0/Download",
                        "/storage/emulated/0"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".pdf", ".doc", ".docx", ".xls", ".xlsx",
                        ".ppt", ".pptx", ".txt", ".epub", ".csv"};
                s.junkType = JunkItem.TYPE_DOC;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "文档文件，请确认是否可删";
                break;
            case TYPE_ARCHIVE:
                s.roots = new String[]{
                        "/storage/emulated/0",
                        "/storage/emulated/0/Download"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".zip", ".rar", ".7z", ".tar", ".gz", ".bz2"};
                s.junkType = JunkItem.TYPE_ARCHIVE;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "压缩包，请确认是否可删";
                break;
            case TYPE_DOWNLOAD:
                s.roots = new String[]{"/storage/emulated/0/Download"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{}; // 所有文件
                s.junkType = JunkItem.TYPE_DOWNLOAD;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "下载目录文件，请确认是否可删";
                break;
            case TYPE_EMPTY_FILE:
                s.roots = new String[]{
                        "/storage/emulated/0",
                        "/storage/emulated/0/Android/data",
                        "/storage/emulated/0/Download"};
                s.mode = MODE_EMPTY;
                s.patterns = new String[]{};
                s.junkType = JunkItem.TYPE_EMPTY_FILE;
                s.reason = "0 字节的空文件，可安全清理";
                break;
            case TYPE_BLUETOOTH:
                s.roots = new String[]{
                        "/storage/emulated/0/Bluetooth",
                        "/storage/emulated/0/bluetooth"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{}; // 所有文件
                s.junkType = JunkItem.TYPE_BLUETOOTH;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "蓝牙接收的文件，请确认是否可删";
                break;
            case TYPE_RECORD:
                s.roots = new String[]{
                        "/storage/emulated/0",
                        "/storage/emulated/0/Sounds",
                        "/storage/emulated/0/Recordings",
                        "/storage/emulated/0/CallRecord"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".amr", ".3gpp", ".m4a", ".wav"};
                s.junkType = JunkItem.TYPE_RECORD;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "录音/通话录音文件，请确认是否可删";
                break;
            case TYPE_WALLPAPER:
                s.roots = new String[]{
                        "/storage/emulated/0/Android/data/com.android.wallpaper",
                        "/storage/emulated/0/Android/data/com.miui.home",
                        "/storage/emulated/0/Android/data/com.android.systemui"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"wallpaper", "downloaded_wallpaper",
                        "lockwallpaper", "Magazines", "wallpaper_cache"};
                s.junkType = JunkItem.TYPE_WALLPAPER;
                s.reason = "壁纸缓存，清理后自动恢复默认壁纸";
                break;
            case TYPE_PHOTO:
                s.roots = new String[]{"/storage/emulated/0/DCIM",
                        "/storage/emulated/0/Pictures",
                        "/storage/emulated/0/Download"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".jpg", ".jpeg", ".png", ".bmp",
                        ".webp", ".gif", ".heic"};
                s.junkType = JunkItem.TYPE_PHOTO;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "照片/图片文件，请确认是否可删（建议保留）";
                break;
            case TYPE_EBOOK:
                s.roots = new String[]{"/storage/emulated/0/Documents",
                        "/storage/emulated/0/Books",
                        "/storage/emulated/0/Download",
                        "/storage/emulated/0"};
                s.mode = MODE_FILE;
                s.patterns = new String[]{".epub", ".mobi", ".azw", ".azw3",
                        ".txt", ".pdf"};
                s.junkType = JunkItem.TYPE_EBOOK;
                s.advice = JunkItem.ADVICE_KEEP;
                s.reason = "电子书/文档文件，请确认是否可删";
                break;
            case TYPE_APP_DATA:
                s.roots = new String[]{"/storage/emulated/0/Android/data"};
                s.mode = MODE_DIR;
                s.patterns = new String[]{"cache", "files", "databases",
                        "shared_prefs", "tmp", ".cache"};
                s.junkType = JunkItem.TYPE_APP_CACHE_ALL;
                s.advice = JunkItem.ADVICE_CLEAN;
                s.reason = "应用私有数据缓存，清理后应用可能需重新登录/加载";
                break;
            default:
                return null;
        }
        return s;
    }

    /** 扫描指定类型的可清理项。 */
    public static List<JunkItem> scan(Context ctx, int type, RootShell root) {
        if (type == TYPE_RESIDUAL) return scanResidual(ctx, root);
        if (type == TYPE_DEEP) return scanDeep(ctx, root);
        Spec s = getSpec(type);
        if (s == null) return new ArrayList<>();
        List<JunkItem> items = new ArrayList<>();
        if (root != null && root.isAvailable()) {
            scanWithRoot(s, root, items);
        } else {
            scanWithFile(s, items);
            scanMediaStore(ctx, s, items); // 无 root 兜底：MediaStore 公共媒体/文件
        }
        return items;
    }

    /** 深度清理：聚合扫描多类可清理项，去重合并。 */
    private static List<JunkItem> scanDeep(Context ctx, RootShell root) {
        int[] types = {TYPE_CACHE, TYPE_THUMB, TYPE_TEMP, TYPE_LOG, TYPE_AD,
                TYPE_RESIDUAL, TYPE_EMPTY_FILE, TYPE_LARGE, TYPE_APK,
                TYPE_SCREENSHOT, TYPE_SYSTEM_JUNK, TYPE_APP_CACHE, TYPE_DOWNLOAD,
                TYPE_MUSIC, TYPE_VIDEO_FILE, TYPE_DOC, TYPE_ARCHIVE,
                TYPE_BLUETOOTH, TYPE_RECORD, TYPE_PHOTO, TYPE_EBOOK};
        List<JunkItem> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int t : types) {
            for (JunkItem it : scan(ctx, t, root)) {
                if (it.path != null && seen.add(it.path)) all.add(it);
            }
        }
        return all;
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
        } else if (s.mode == MODE_EMPTY) {
            StringBuilder find = new StringBuilder("find");
            for (String r : s.roots) find.append(" '").append(r).append("'");
            find.append(" -type f -empty -printf '%s\\t%p\\n' 2>/dev/null");
            parseLines(root.run(find.toString()), s, items);
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
            else if (s.mode == MODE_EMPTY) collectEmpty(rootDir, s, items, 0);
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

    private static void collectEmpty(File dir, Spec s, List<JunkItem> out, int depth) {
        if (depth > MAX_DEPTH || dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectEmpty(f, s, out, depth + 1);
            } else if (f.length() == 0) {
                JunkItem it = new JunkItem();
                it.path = f.getAbsolutePath();
                it.size = 0;
                it.type = s.junkType;
                it.advice = s.advice;
                it.reason = s.reason;
                out.add(it);
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

    /**
     * 无 root 兜底扫描：通过 MediaStore 查询公共媒体/文件，覆盖截图、视频、音乐、
     * 文档、压缩包、下载、安装包、临时文件等。仅对按扩展名匹配（非目录名）的类型有效；
     * 目录名匹配（社交/应用缓存）因 /Android/data 受限无法访问，交由 root 分支处理。
     */
    @SuppressWarnings("deprecation")
    private static void scanMediaStore(Context ctx, Spec s, List<JunkItem> items) {
        if (ctx == null || s.mode == MODE_DIR) return;
        ContentResolver cr = ctx.getContentResolver();
        Uri uri = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Files.getContentUri("external");
        String[] proj = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.SIZE};
        StringBuilder sel = new StringBuilder();
        List<String> args = new ArrayList<>();
        boolean has = false;
        if (s.patterns != null && s.patterns.length > 0) {
            sel.append("(");
            for (int i = 0; i < s.patterns.length; i++) {
                if (i > 0) sel.append(" OR ");
                sel.append(MediaStore.Files.FileColumns.DATA).append(" LIKE ?");
                args.add("%" + s.patterns[i]);
            }
            sel.append(")");
            has = true;
        }
        if (s.roots != null) {
            for (String r : s.roots) {
                if (r.startsWith("/storage/emulated/0/")
                        && !r.contains("/Android/data/")
                        && !r.contains("/Android/obb/")
                        && !r.contains("/Android/media/")) {
                    if (has) sel.append(" AND ");
                    sel.append(MediaStore.Files.FileColumns.DATA).append(" LIKE ?");
                    args.add(r + "%");
                    has = true;
                }
            }
        }
        if (!has) return;
        Set<String> existing = new HashSet<>();
        for (JunkItem it : items) if (it.path != null) existing.add(it.path);
        Cursor c = null;
        try {
            c = cr.query(uri, proj, sel.toString(), args.toArray(new String[0]), null);
            if (c == null) return;
            int idxData = c.getColumnIndex(MediaStore.Files.FileColumns.DATA);
            int idxSize = c.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
            if (idxData < 0) return;
            while (c.moveToNext()) {
                String path = c.getString(idxData);
                if (path == null || path.isEmpty() || existing.contains(path)) continue;
                long sz = idxSize >= 0 ? c.getLong(idxSize) : 0;
                if (s.minSize > 0 && sz < s.minSize) continue;
                JunkItem it = new JunkItem();
                it.path = path;
                it.size = sz;
                it.type = s.junkType;
                it.advice = s.advice;
                it.reason = s.reason;
                items.add(it);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
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

    /* ===================== 用户自定义规则 ===================== */
    /**
     * 扫描用户自定义规则。CustomRule.mode: 0=目录名匹配 1=文件后缀匹配 2=空文件。
     */
    public static List<JunkItem> scanCustom(Context ctx, RootShell root, CustomRule rule) {
        Spec s = new Spec();
        s.roots = (rule.roots != null && rule.roots.length > 0)
                ? rule.roots : new String[]{"/storage/emulated/0"};
        s.mode = rule.mode; // 0=DIR 1=FILE 2=EMPTY
        s.patterns = rule.patterns != null ? rule.patterns : new String[]{};
        s.minSize = rule.minSize;
        s.junkType = JunkItem.TYPE_APP_CACHE;
        s.advice = rule.advice;
        s.reason = (rule.reason != null && !rule.reason.isEmpty())
                ? rule.reason : "自定义规则扫描结果";
        List<JunkItem> items = new ArrayList<>();
        if (root != null && root.isAvailable()) scanWithRoot(s, root, items);
        else scanWithFile(s, items);
        return items;
    }
}
