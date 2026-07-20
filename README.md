# 分区清理大师 (Partition Cleaner)

一个 Android **Root 清理工具**，能像“分区详情”界面那样展示各分区使用情况，
并扫描 / 清理系统分区与数据分区的垃圾文件、查询空文件夹。

## 功能

- **分区详情查询**：展示 System / Vendor / Data / Cache / Cust / Magisk 等文件系统分区，
  以及 RAM / Swap / Cached / Slab 等内存区的总量、可用、占比（基于 `StatFs` 与 `/proc/meminfo`）。
- **垃圾扫描**：
  - 用户可访问区（内部存储 `Android/data`、Download、Tencent、缩略图等）的缓存目录、`.log`/`.tmp`/`.bak` 文件。
  - 系统 / 数据分区（需 Root）的日志、临时文件，通过 `find` 命令收集。
- **空文件夹查询**：递归找出指定分区下的空目录（需 Root）。
- **重复文件清理**：扫描内部存储常见目录（DCIM、Download、Pictures、Movies、Documents、Tencent、Android/media、WhatsApp），
  先按文件大小分组、再对同大小文件计算 MD5 校验内容是否真正一致；每组保留你选定的那一份，删除其余副本（默认保留首个）。
  单文件超过 300MB 跳过哈希以避免卡顿；删除时优先用 Root（`su` 执行 `rm`），无 Root 则退化为普通删除。
- **一键清理**：勾选后通过 `su` 执行 `rm -rf`；清理系统分区前自动 `mount -o remount,rw`。
- **安全分级提示**：扫描结果中每一项都标注「建议清理」或「建议保留」并说明原因。
  系统分区（`/system`、`/vendor`、`/cust` 等）、应用私有数据（`/data/data`）、
  以及非用户区的空文件夹，默认标记为「建议保留」（标红、默认不勾选），避免误删变砖；
  普通缓存/日志/临时文件标记为「建议清理」（标绿、默认勾选）。用户仍可手动改勾选。
- **筛选与一键清理**：顶部可按「全部 / 建议清理 / 建议保留」筛选列表；
  底部「清理选中」按勾选删除，「只清建议清理」一键删除所有安全项（无视勾选状态）。

## 重复文件清理

1. 首页点「**查找重复文件**」进入重复文件页。
2. App 递归扫描上述常见目录，先按**文件大小**快速分组（大小不同的不可能重复），再对同大小的候选文件计算 **MD5**，
   只有内容哈希完全一致的才会被归为一组，避免“同名不同内容”误删。
3. 每组以单选框列出所有副本，**默认选中第一个作为保留项**；你可以改成保留任意一个。
4. 点「**清理重复项（保留选中）**」删除每组中除保留项之外的其余副本。删除通过 `su` 执行 `rm`，
   若未授权 Root 则退化为普通文件删除（仅能删应用自身可访问的文件）。
5. 顶部汇总「共 N 组重复，可释放 X MB」。

> 扫描范围刻意限制在用户可访问的内部存储目录，不会碰系统分区；单文件 > 300MB 不参与哈希比对。

## 技术栈

- 语言：Java 17
- UI：传统 View + Material Components（Material3）+ RecyclerView
- 构建：Gradle (AGP 8.5.2) + GitHub Actions 云端编译
- 最低支持：Android 5.0 (API 21)

## 本地构建（可选）

需要本机安装 Android SDK（API 34）与 Gradle 8.9：

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0"
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

> 若工程无 `gradlew` 包装脚本，可直接用 Gradle 8.9 运行 `gradle assembleDebug`。

## 云端构建（推荐，零本地环境）

1. 把本工程推到 GitHub 仓库。
2. 在仓库 **Actions → Build APK → Run workflow** 手动触发，或 push 到 `main`/`master` 自动触发。
3. 构建完成后在 **Artifacts** 下载 `partition-cleaner-apk`（含 `app-debug.apk`）。
4. 打 `v*` 标签推送（`git tag v1.0.0 && git push origin v1.0.0`）会自动发布到 **Releases**。

推送到 GitHub 的示例命令：

```bash
git init
git add -A
git commit -m "partition cleaner app"
git branch -M main
git remote add origin https://github.com/<你的用户名>/partition-cleaner.git
git push -u origin main
```

## 使用说明

1. 安装 APK（允许“未知来源”）。
2. 打开 App，会请求 Root 授权（Magisk/KernelSU 等）。已授权则显示“Root 已授权”。
3. 首页展示各分区详情。
4. 点“**扫描垃圾**”扫描可清理项；点“**查询空文件夹**”仅列出空目录。
5. 勾选要清理的项，点“**清理选中**”。系统分区文件需 Root 才会真正删除。
6. 点「**查找重复文件**」扫描内部存储的重复内容；每组勾选要保留的那一份，点「**清理重复项**」删除其余副本。

## 风险提示

- 清理**系统分区**（`/system`、`/vendor`、`/cust` 等）存在风险，误删可能导致系统无法启动。
  请仅勾选明确是垃圾的日志/缓存/临时文件，不要勾选不认识的系统目录。
- 清理前会自动以读写方式重新挂载相关分区；清理后建议重启验证系统正常。
- 无 Root 时仅能扫描/清理应用自身可访问的用户区文件，无法触及系统分区。

## 已知限制

- 内存区（Cached / Slab）以 `MemTotal` 为展示基线呈现占比，仅作参考，不能清理。
- Android 11+ 的 Scoped Storage 限制下，普通权限无法列举其他应用的 `Android/data` 子目录，
  需 Root 才能完整扫描。
- 本工具不提供“自动全盘清理”，所有删除均需人工勾选确认。
