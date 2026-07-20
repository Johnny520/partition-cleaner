package com.example.partitioncleaner;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

/**
 * 通过 su 获取 root shell 并执行命令。
 * 同一进程的输入/输出流在整个生命周期内复用，用随机 marker 标记命令输出结束。
 */
public class RootShell implements IShell {
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

    /**
     * 检测系统是否安装了 root 管理器（Magisk/KernelSU/SuperSU）。
     * 仅检查 su 二进制是否存在，不会触发授权弹窗。
     */
    public static boolean suBinaryExists() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "command -v su"});
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            br.close();
            p.destroy();
            return line != null && !line.trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
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
