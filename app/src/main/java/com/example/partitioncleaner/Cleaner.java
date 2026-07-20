package com.example.partitioncleaner;

import java.util.List;

/** 执行垃圾清理（需要 root 才能删除系统分区文件）。 */
public class Cleaner {

    /** 清理选中的项（it.selected 为 true 的），返回结果摘要。 */
    public static String clean(IShell shell, List<JunkItem> items) {
        return cleanInternal(shell, items, true);
    }

    /** 无视勾选状态，清理列表中所有项（用于“只清建议清理”）。 */
    public static String cleanForce(IShell shell, List<JunkItem> items) {
        return cleanInternal(shell, items, false);
    }

    private static String cleanInternal(IShell shell, List<JunkItem> items, boolean respectSelection) {
        if (shell == null || !shell.isAvailable()) {
            return "未获取 Root / 免 Root（Shizuku）权限，无法清理系统分区";
        }
        int ok = 0;
        int fail = 0;
        for (JunkItem it : items) {
            if (respectSelection && !it.selected) continue;
            String r = cleanOne(shell, it);
            if (r.contains("OK")) ok++;
            else fail++;
        }
        return "清理完成：成功 " + ok + " 项，失败 " + fail + " 项";
    }

    private static String cleanOne(IShell shell, JunkItem it) {
        // 系统分区需先以读写方式重新挂载
        String mp = mountPointOf(it.path);
        if (mp != null) {
            shell.run("mount -o remount,rw " + mp + " 2>/dev/null");
        }
        String rm;
        if (it.type == JunkItem.TYPE_EMPTY_DIR) {
            rm = "rmdir '" + it.path + "' 2>/dev/null || rm -rf '" + it.path + "' 2>/dev/null";
        } else {
            rm = "rm -rf '" + it.path + "' 2>/dev/null";
        }
        return shell.run(rm + " && echo OK || echo FAIL");
    }

    /** 找出路径所属的系统分区挂载点（用于 remount）。 */
    private static String mountPointOf(String path) {
        String[] sys = {"/system", "/system_ext", "/vendor", "/product", "/odm", "/cust"};
        for (String s : sys) {
            if (path.equals(s) || path.startsWith(s + "/")) {
                return s;
            }
        }
        return null;
    }
}
