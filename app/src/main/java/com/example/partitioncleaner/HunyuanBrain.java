/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.os.Build;

import java.util.ArrayList;
import java.util.List;

/**
 * 文强哥内置的混元 AI 助手（本地离线版）。
 * 不依赖网络，根据规则 + 设备信息 + 简易算式解析生成回答。
 */
public class HunyuanBrain {

    public String answer(String raw) {
        if (raw == null) return defaultReply();
        String q = raw.trim();
        if (q.isEmpty()) return defaultReply();
        String lower = q.toLowerCase();

        // 算术
        if (looksLikeMath(q) || lower.contains("算") || lower.contains("计算") || lower.contains("等于")) {
            Double r = evaluate(q);
            if (r != null && (lower.contains("算") || lower.contains("计算") || lower.contains("等于") || looksLikeMath(q))) {
                if (r != null) {
                    String expr = extractExpr(q);
                    return "计算结果：" + (expr.isEmpty() ? "" : expr + " = ") + format(r);
                }
            }
        }

        // 问候
        if (lower.matches(".*(你好|您好|hi|hello|在吗|在不在|哈喽).*")) {
            return "你好呀，我是文强哥内置的混元 AI 助手 ✨\n有什么可以帮你的？你可以问我清理建议、Root 获取方法、设备情况，或者让我帮你算道题。";
        }
        if (lower.matches(".*(谢谢|感谢|thx|thank).*")) {
            return "不客气～有问题随时找我 😊";
        }

        // Root
        if (lower.contains("root") || lower.contains("权限") || lower.contains("超级用户") || lower.contains("面具") || lower.contains("magisk")) {
            return rootGuide();
        }

        // 清理 / 垃圾
        if (lower.contains("清理") || lower.contains("垃圾") || lower.contains("空间") || lower.contains("卡") || lower.contains("慢") || lower.contains("建议")) {
            return cleanGuide();
        }

        // 设备
        if (lower.contains("设备") || lower.contains("手机") || lower.contains("型号") || lower.contains("配置") || lower.contains("我的") || lower.contains("怎么样") || lower.contains("如何")) {
            return deviceOverview();
        }

        // 工具推荐
        if (lower.contains("工具") || lower.contains("推荐") || lower.contains("功能") || lower.contains("能干嘛") || lower.contains("做什么")) {
            return toolGuide();
        }

        if (lower.contains("主题") || lower.contains("换肤") || lower.contains("皮肤") || lower.contains("颜色")) {
            return "在「用户」页点「主题管理」即可切换 5 套主题：翡翠绿、深海蓝、魅影紫、炽焰红、曜金黑，选中即时预览并自动记忆。";
        }

        if (lower.contains("关于") || lower.contains("作者") || lower.contains("文强哥")) {
            return "本应用由 文强哥（GitHub：Johnny520） 开发，版权 © 2026。可在「用户 → 关于应用」查看详细信息与鸣谢。";
        }

        return defaultReply();
    }

    private String rootGuide() {
        return "获取 Root 权限常见方式（以 Magisk 为例）：\n" +
                "1. 解锁 Bootloader（各品牌方法不同，会清空数据，请备份）。\n" +
                "2. 刷入第三方 Recovery（如 TWRP）。\n" +
                "3. 在 Recovery 中刷入 Magisk 卡刷包。\n" +
                "4. 重启后安装 Magisk App 完成授权。\n" +
                "⚠️ Root 会失去保修、可能变砖，请谨慎操作，仅在清楚风险时尝试。本应用的深度清理功能需要 Root 才能访问系统分区。";
    }

    private String cleanGuide() {
        return "想腾出空间、让手机更流畅，可以这样做：\n" +
                "· 用「垃圾清理」扫掉缓存与残留文件；\n" +
                "· 用「空文件夹」清理无用的空目录；\n" +
                "· 用「重复文件」找出并删除重复照片/文件；\n" +
                "· 卸载长期不用的 App；\n" +
                "· 把大文件转移到电脑或云盘。\n" +
                "建议每周清理一次。需要我帮你开始扫描吗？";
    }

    private String deviceOverview() {
        return "你的设备概览（本地读取）：\n" +
                "· 品牌机型：" + safe(Build.BRAND) + " " + safe(Build.MODEL) + "\n" +
                "· 系统版本：Android " + safe(Build.VERSION.RELEASE) + "（SDK " + Build.VERSION.SDK_INT + "）\n" +
                "· 处理器：" + safe(Build.HARDWARE) + "，核心数 " + Runtime.getRuntime().availableProcessors() + "\n" +
                "· 屏幕/传感器等信息可在「主页」仪表盘查看。\n" +
                "如果觉得卡顿，可以试试上面的清理建议～";
    }

    private String toolGuide() {
        return "工具箱里有 6 个实用小工具：\n" +
                "🧮 计算器 · 🔄 单位换算 · 📱 设备信息\n" +
                "📝 文本工具 · 🎲 随机数 · 🕒 时间戳\n" +
                "另外还有垃圾清理、空文件夹、重复文件、分区浏览，以及我（混元 AI 助手）～";
    }

    private String defaultReply() {
        return "我是文强哥内置的混元 AI 助手 ✨（本地离线运行，无需联网）。\n" +
                "你可以问我：\n" +
                "· 「怎么清理垃圾 / 释放空间」\n" +
                "· 「怎么获取 Root 权限」\n" +
                "· 「我的设备怎么样」\n" +
                "· 「推荐一些工具」\n" +
                "· 直接发算式如 25*36 让我帮你算。";
    }

    private String safe(String s) {
        return (s == null || s.isEmpty()) ? "未知" : s;
    }

    /* ---------------- 简易算式解析 ---------------- */

    private boolean looksLikeMath(String q) {
        return q.matches(".*[0-9].*[+\\-*/%^].*") || q.matches(".*[+\\-*/%^].*[0-9].*");
    }

    private String extractExpr(String q) {
        StringBuilder sb = new StringBuilder();
        for (char c : q.toCharArray()) {
            if ("0123456789.+-*/%^() ".indexOf(c) >= 0) sb.append(c);
        }
        return sb.toString().trim();
    }

    /** 解析并计算算式，失败返回 null。支持 + - * / % ^ 和括号。 */
    private Double evaluate(String raw) {
        try {
            String expr = extractExpr(raw);
            if (expr.isEmpty()) return null;
            List<String> tokens = tokenize(expr);
            // 转 RPN
            List<String> output = new ArrayList<>();
            List<String> opStack = new ArrayList<>();
            int i = 0;
            while (i < tokens.size()) {
                String t = tokens.get(i);
                if (isNumber(t)) {
                    output.add(t);
                } else if (t.equals("(")) {
                    opStack.add(t);
                } else if (t.equals(")")) {
                    while (!opStack.isEmpty() && !opStack.get(opStack.size() - 1).equals("(")) {
                        output.add(opStack.remove(opStack.size() - 1));
                    }
                    if (!opStack.isEmpty()) opStack.remove(opStack.size() - 1);
                } else {
                    while (!opStack.isEmpty() && !opStack.get(opStack.size() - 1).equals("(")
                            && prec(opStack.get(opStack.size() - 1)) >= prec(t)) {
                        output.add(opStack.remove(opStack.size() - 1));
                    }
                    opStack.add(t);
                }
                i++;
            }
            while (!opStack.isEmpty()) output.add(opStack.remove(opStack.size() - 1));

            // 计算 RPN
            List<Double> stack = new ArrayList<>();
            for (String t : output) {
                if (isNumber(t)) {
                    stack.add(Double.parseDouble(t));
                } else {
                    if (stack.size() < 2) return null;
                    double b = stack.remove(stack.size() - 1);
                    double a = stack.remove(stack.size() - 1);
                    stack.add(apply(t, a, b));
                }
            }
            if (stack.size() != 1) return null;
            return stack.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private double apply(String op, double a, double b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/": return b == 0 ? 0 : a / b;
            case "%": return b == 0 ? 0 : a % b;
            case "^": return Math.pow(a, b);
            default: return 0;
        }
    }

    private int prec(String op) {
        switch (op) {
            case "+": case "-": return 1;
            case "*": case "/": case "%": return 2;
            case "^": return 3;
            default: return 0;
        }
    }

    private List<String> tokenize(String expr) {
        List<String> tokens = new ArrayList<>();
        int i = 0;
        while (i < expr.length()) {
            char c = expr.charAt(i);
            if (c == ' ') { i++; continue; }
            if ("+-*/%^()".indexOf(c) >= 0) {
                tokens.add(String.valueOf(c));
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i++));
                }
                tokens.add(sb.toString());
            } else {
                i++;
            }
        }
        return tokens;
    }

    private boolean isNumber(String t) {
        try {
            Double.parseDouble(t);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String format(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.format("%.4f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
