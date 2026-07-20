package com.example.partitioncleaner;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Shizuku 免 Root 桥接（纯反射实现，编译期不依赖 Shizuku SDK）。
 *
 * 能力：
 *  · isShizukuAvailable() —— 检测 Shizuku 管理器已安装且本应用已获得 API_V23 权限；
 *  · requestShizuku()     —— 拉起 Shizuku 管理器，引导用户在里面授权；
 *  · getShell()           —— 取得一个以 shell(uid 2000) 身份运行的 IShell，用于免 Root 删除媒体文件；
 *  · getBestShell()       —— 优先 Root，没有 Root 时退回到 Shizuku。
 *
 * 注意：Shizuku 的 shell 身份(uid 2000)可访问 /storage/emulated/0 等共享存储，
 * 但无法访问 /system 等系统分区（那仍需 Root）。在真机上的授权与执行需配合 Shizuku 管理器验证。
 */
public class ShizukuAccess {

    public static final String SHIZUKU_PKG = "moe.shizuku.manager";
    public static final String SHIZUKU_PERMISSION = "moe.shizuku.manager.permission.API_V23";

    /** Shizuku 管理器是否已安装。 */
    public static boolean isManagerInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 本应用是否已获得 Shizuku 的 API_V23 权限。 */
    public static boolean isPermissionGranted(Context ctx) {
        return ctx.checkSelfPermission(SHIZUKU_PERMISSION) == PackageManager.PERMISSION_GRANTED;
    }

    /** 综合判断：管理器已装 + 已授权。 */
    public static boolean isShizukuAvailable(Context ctx) {
        return isManagerInstalled(ctx) && isPermissionGranted(ctx);
    }

    /** 拉起 Shizuku 管理器，引导用户授权（API_V23）。 */
    public static void requestShizuku(Context ctx) {
        try {
            Intent i = ctx.getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
            if (i != null) {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 取得一个 Shizuku 提供的 shell 进程（以 shell uid 运行）。
     * 通过反射调用 rikka.shizuku.Shizuku.newProcess() 实现；任何失败返回 null。
     */
    public static IShell getShell(Context ctx) {
        if (!isShizukuAvailable(ctx)) return null;
        try {
            Class<?> shizuku = Class.forName("rikka.shizuku.Shizuku");
            Method ping = shizuku.getMethod("pingBinder");
            if (!(Boolean) ping.invoke(null)) return null;
            Method newProcess = shizuku.getMethod("newProcess", String[].class, String[].class, String.class);
            Process p = (Process) newProcess.invoke(null, new String[]{"sh"}, null, null);
            return new ShizukuShell(p);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 优先 Root，否则退回到 Shizuku。两者皆无则返回 null。 */
    public static IShell getBestShell(Context ctx) {
        RootShell root = new RootShell();
        if (root.requestRoot() && root.isAvailable()) return root;
        root.close();
        return getShell(ctx);
    }

    /** 以 shell 身份运行的 IShell 实现（镜像 RootShell 的 stdin/stdout + marker 机制）。 */
    private static class ShizukuShell implements IShell {
        private final Process process;
        private final DataOutputStream os;
        private final BufferedReader out;
        private boolean available;

        ShizukuShell(Process p) {
            this.process = p;
            try {
                os = new DataOutputStream(p.getOutputStream());
                out = new BufferedReader(new InputStreamReader(p.getInputStream()));
                os.writeBytes("id\n");
                os.flush();
                String line = out.readLine();
                available = line != null && (line.contains("uid=2000") || line.contains("uid=0"));
            } catch (Exception e) {
                available = false;
            }
        }

        @Override
        public boolean isAvailable() {
            return available;
        }

        @Override
        public String run(String cmd) {
            if (!available) return "";
            try {
                os.writeBytes(cmd + "\n");
                os.flush();
                String marker = "===SDONE" + System.currentTimeMillis() + "===";
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
    }
}
