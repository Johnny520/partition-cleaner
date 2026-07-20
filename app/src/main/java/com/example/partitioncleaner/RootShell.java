package com.example.partitioncleaner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * 通过 su 获取 root shell 并执行命令。
 * 同一进程的输入/输出流在整个生命周期内复用，用随机 marker 标记命令输出结束。
 */
public class RootShell {
    private Process process;
    private DataOutputStream os;
    private BufferedReader out;
    private boolean available;

    /** 尝试获取 root 权限，成功返回 true。 */
    public boolean requestRoot() {
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            out = new BufferedReader(new InputStreamReader(process.getInputStream()));
            os.writeBytes("id\n");
            os.flush();
            String line = out.readLine();
            available = line != null && line.contains("uid=0");
            return available;
        } catch (Exception e) {
            available = false;
            return false;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /** 执行一条命令并返回其 stdout（自动剔除结束 marker 行）。 */
    public String run(String cmd) {
        if (!available) return "";
        try {
            os.writeBytes(cmd + "\n");
            os.flush();
            String marker = "===DONE" + System.currentTimeMillis() + "===";
            os.writeBytes("echo " + marker + "\n");
            os.flush();
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = out.readLine()) != null) {
                if (line.equals(marker)) break;
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void close() {
        try {
            if (os != null) {
                os.writeBytes("exit\n");
                os.flush();
                os.close();
            }
            if (process != null) process.destroy();
        } catch (Exception ignored) {
        }
    }
}
