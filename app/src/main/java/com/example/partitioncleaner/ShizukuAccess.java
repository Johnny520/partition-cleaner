package com.example.partitioncleaner;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

/**
 * Shizuku 免 Root 桥接（基于官方 Shizuku SDK）。
 *
 * 关键点：
 *  · 必须在 AndroidManifest 中声明 rikka.shizuku.ShizukuProvider，本应用才会出现在
 *    Shizuku 管理器的“应用”列表里，用户才能授予 API_V23 权限；
 *  · getShell() 通过 Shizuku.newProcess("sh") 取得一个以 shell(uid 2000) 身份运行的进程，
 *    用于免 Root 删除媒体文件等。shell 身份可访问共享存储，但无法访问 /system（那仍需 Root）。
 */
public class ShizukuAccess {

    public static final String SHIZUKU_PKG = "moe.shizuku.manager";
    public static final String SHIZUKU_PERMISSION = "moe.shizuku.manager.permission.API_V23";
    private static final int REQ_CODE = 9527;

    /** 授权结果回调。 */
    public interface PermissionCallback {
        void onResult(boolean granted);
    }

    /** Shizuku 管理器是否已安装。 */
    public static boolean isManagerInstalled(Context ctx) {
        try {
            ctx.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Shizuku 服务 binder 是否已就绪（管理器已运行且本应用已连上）。 */
    public static boolean isBinderAvailable() {
        try {
            return Shizuku.pingBinder();
        } catch (Throwable t) {
            return false;
        }
    }

    /** 本应用是否已获得 Shizuku 的 API_V23 权限。 */
    public static boolean isPermissionGranted(Context ctx) {
        try {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable t) {
            return false;
        }
    }

    /** 综合判断：管理器已装 + binder 就绪 + 已授权。 */
    public static boolean isShizukuAvailable(Context ctx) {
        return isManagerInstalled(ctx) && isBinderAvailable() && isPermissionGranted(ctx);
    }

    /**
     * 请求 Shizuku 授权（应用内弹窗）。若已授权或 binder 未就绪会直接回调。
     * 注意：必须先由用户在 Shizuku 管理器里把本应用列入并授权，否则此处会提示失败。
     */
    public static void requestPermission(PermissionCallback cb) {
        if (isPermissionGranted(null)) {
            if (cb != null) cb.onResult(true);
            return;
        }
        Shizuku.OnRequestPermissionResultListener listener = new Shizuku.OnRequestPermissionResultListener() {
            @Override
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                Shizuku.removeRequestPermissionResultListener(this);
                if (cb != null) cb.onResult(grantResult == PackageManager.PERMISSION_GRANTED);
            }
        };
        Shizuku.addRequestPermissionResultListener(listener);
        try {
            Shizuku.requestPermission(REQ_CODE);
        } catch (Throwable t) {
            Shizuku.removeRequestPermissionResultListener(listener);
            if (cb != null) cb.onResult(false);
        }
    }

    /** 拉起 Shizuku 管理器（用于安装/启动/手动授权）。 */
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

    /** 取得一个 Shizuku 提供的 shell 进程（以 shell uid 运行）。未授权返回 null。 */
    public static IShell getShell(Context ctx) {
        if (!isShizukuAvailable(ctx)) return null;
        try {
            // Shizuku 13.x 把 newProcess 设为 private，但方法稳定存在；通过一次反射取得远程进程
            // （返回 ShizukuRemoteProcess，本身 extends Process），作为 shell 使用。
            Method m = Shizuku.class.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            Process p = (Process) m.invoke(null, new String[]{"sh"}, null, null);
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
        private DataOutputStream os;
        private BufferedReader out;
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
