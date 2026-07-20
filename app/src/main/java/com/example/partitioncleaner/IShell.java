package com.example.partitioncleaner;

/**
 * 统一的命令执行接口。Root（su）与 Shizuku 免 Root 都实现它，
 * 清理/扫描逻辑只依赖该接口，从而支持在没有 Root 时降级到 Shizuku。
 */
public interface IShell {

    /** 当前 shell 是否可用（已取得 root 或 Shizuku 授权）。 */
    boolean isAvailable();

    /** 执行一条命令，返回其 stdout（失败/不可用返回空字符串）。 */
    String run(String cmd);
}
