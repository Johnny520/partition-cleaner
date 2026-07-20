# 更新日志 / Changelog

本文件记录分区清理大师每个正式版本的变更。Release 同时会在 GitHub 自动生成发行说明。

## v1.0.11（2026-07-21）
- 🏢 **企业信息查询重写（官方·免费·最全·不丑）**：原 Bing 网页抓取常因反爬改版返回空结果（"查不到"），改为**内嵌国家企业信用信息公示系统（gsxt.gov.cn）官方页面**——WebView 真实浏览器自动过 JS 挑战(WAF)，数据官方免费最全；注入 JS 提取企业列表后，**App 原生 Material 卡片结构化展示**（自动归类/去重/按名称排序/可点击），WebView 同时作为详情兜底。再也不是难看的纯网页
- 📦 **修复「安装包提取只显示几个应用」**：根因是 Android 11+ 包可见性限制——`AndroidManifest.xml` 的 `<queries>` 仅声明了 Shizuku 一个包，未申请 `QUERY_ALL_PACKAGES` 时 `getInstalledApplications()` 默认只返回自身+系统核心。现已加 `QUERY_ALL_PACKAGES` 权限并补充 launcher intent，`ApkExtractActivity` 可列出全部已装应用
- ℹ️ 已知问题/闪退警示：v1.0.0–v1.0.9 因 `ShizukuProvider` 被误设 `exported="false"`（应为 `true`），**打开即闪退**，已于 v1.0.10 修复；v1.0.8 首构建失败已修

## v1.0.10（2026-07-21）
- 🐞 **修复打开即闪退（真正的元凶）**：`AndroidManifest.xml` 中 `rikka.shizuku.ShizukuProvider` 被误设为 `android:exported="false"`，而 Shizuku 要求必须为 `true`，导致 ContentProvider 在 Application 初始化前就抛 `IllegalStateException` 崩溃。改为 `exported="true"` 后恢复正常
- ℹ️ 该崩溃发生在 `App.onCreate` 之前，所以此前任何“崩溃日志/复制剪贴板”都来不及执行——日志拿不到是表象，根因即此

## v1.0.9（2026-07-21）
- 📂 **崩溃日志改写到公开 Download 目录（MediaStore）**：Android 11+ 隐藏了 `/Android/data/`，此前日志“写了但拿不到”；现在文件管理器里直接可见 `分区清理大师_崩溃日志_*.txt`，长按分享/发送即可
- 📋 保留自动复制到剪切板（部分机型不留存，故以 Download 文件为准）
- ℹ️ 仍保留 `/storage/emulated/0/分区清理大师/log` 与 `/Android/data/...` 保底写入

## v1.0.8（2026-07-21）
- 🏢 **恢复「企业信息查询」功能**：此前移除它并未解决闪退，故复原。该功能仅在手动点入时才初始化，与“打开即闪退”无关
- 📋 **崩溃日志改为自动复制到剪切板**：闪退瞬间把完整堆栈写入系统剪切板，打开任意 App（微信/备忘录等）长按粘贴即可发我，**无需再找日志文件**（Android 11+ 已隐藏 `/Android/data/`）
- 🐞 保留文件落盘：崩溃仍同时写入 `/storage/emulated/0/分区清理大师/log` 作为保底
- ℹ️ 关于页保留「导出崩溃日志」按钮

## v1.0.7（2026-07-21）
- 🗑️ 移除「企业信息查询」功能（含 CompanyFetcher / EnterpriseActivity 及相关布局与字符串）
- 🐞 保留内置全局崩溃日志：闪退自动写入 `/storage/emulated/0/分区清理大师/log`
- 📋 关于页新增「导出崩溃日志」：一键复制最近一次崩溃堆栈，便于反馈
- ℹ️ 关于页动态显示版本号

## v1.0.6（2026-07-21）
- 🧹 **微信式四 Tab 布局**：清理 / 概览 / 文件 / 资料，底部导航切换
- 📁 **文件管理**：文件可点击查看，长按跳转所在目录
- 👤 **可自定义资料页**：头像 / 昵称 / 个性签名均可自定义
- 🤖 **AI 助手多模型在线**：混元 / DeepSeek / Kimi / 千问 / 豆包，应用内填 Key 即用
- 🏢 **企业信息查询**：Bing 免费网页抓取兜底（无需 API Key）+ 官网核验
- 🛠️ **源码打包工具链**：资料页入口，提供 JDK / Gradle / SDK / 克隆 / 构建说明
- 🐞 **内置全局崩溃日志**：闪退时自动写入 `/storage/emulated/0/分区清理大师/log`
- ℹ️ 关于页动态显示版本号

## v1.0.5
- 基础清理、重复文件、分区信息、工具箱等既有功能
