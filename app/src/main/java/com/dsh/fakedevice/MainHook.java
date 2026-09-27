package com.dsh.fakedevice;

import android.content.res.Resources;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** 「自欺欺人」模块：只改 com.android.settings（关于本机）显示。纯运行时 hook，关闭模块即还原。 */
public class MainHook implements IXposedHookLoadPackage {

    /** 存储空间整行替换 */
    private static final String FAKE_STORAGE = "114514 ZB / TREE(3) YB / ∞ (阿列夫一) ZB";

    /** 整串规则（长串优先，避免拼出怪东西） */
    private static final String[][] FULL_RULES = {
            {"骁龙®8至尊版移动平台", "AMD Ryzen 9 9950X3D"},
            {"骁龙®8至尊版", "AMD Ryzen 9 9950X3D"},
            {"骁龙8至尊版移动平台", "AMD Ryzen 9 9950X3D"},
            {"高通骁龙8至尊版", "AMD Ryzen 9 9950X3D"},
            {"一加 Ace 6", "ONEPLUS NEVERSETTLE"},
    };

    /** 关键词规则 */
    private static final String[][] KEY_RULES = {
            {"潮汐引擎", "中国嫦娥空间站"},
            {"风驰游戏内核", "NVIDIA GEFORCE"},
            {"电竞三芯", "AMD RADEON"},
            {"极速高刷", "INTEL IRIS XE ARC"},
            {"冰川电池", " "},
            {"万像素", "哈勃望远镜"},
    };

    /** 电池容量行：含 mAh 整行替换 */
    private static final Pattern BATTERY_LINE =
            Pattern.compile(".*mAh.*", Pattern.CASE_INSENSITIVE);

    /** 运行内存行：16 / 16.0 GB */
    private static final Pattern RAM_LINE =
            Pattern.compile("^\\s*16(?:\\.0+)?\\s*GB\\s*$", Pattern.CASE_INSENSITIVE);

    /** 存储卡片：真实已用 / 总容量 */
    private static final Pattern STORAGE_LINE = Pattern.compile(
            "^(.*?)\\s*/\\s*\\d+(?:\\.\\d+)?\\s*(?:GB|TB|MB|KB)\\s*$", Pattern.CASE_INSENSITIVE);

    /** 软件版本行（PLQ110_...） */
    private static final Pattern VERSION_LINE =
            Pattern.compile(".*PLQ110.*", Pattern.CASE_INSENSITIVE);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.android.settings".equals(lp.packageName)) return;
        XposedBridge.log("[FakeDeviceInfo] loaded in " + lp.packageName);

        // 1) 资源文本路径（setText(资源ID) / getString / getText，含 Spanned）
        hookRes("getString", int.class);
        hookRes("getText", int.class);
        hookRes("getString", int.class, Object[].class);

        // 2) 直接设置 CharSequence 的路径
        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                Object a = p.args[0];
                if (a == null) return;
                String src = a.toString();
                String dst = transform(src);
                if (!dst.equals(src)) p.args[0] = dst;
            }
        });
    }

    /** 资源文本 hook：String 与 CharSequence(Spanned) 都处理 */
    private static void hookRes(String name, Class<?>... args) {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, name, args, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam p) {
                    Object r = p.getResult();
                    if (r == null) return;
                    String s = r.toString();
                    String t = transform(s);
                    if (!t.equals(s)) p.setResult(t);
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[FakeDeviceInfo] hookRes " + name + " failed: " + t);
        }
    }

    private static String transform(String s) {
        if (s == null || s.isEmpty()) return s;
        // 归一化空格（全角 / 不换行空格）
        String out = s.replace('\u00A0', ' ').replace('\u3000', ' ').trim();

        // ---- 摄像头两行（整行重写）----
        if (out.startsWith("前置")) return "前置 哈勃望远镜";
        if (out.startsWith("后置")) return "后置 詹姆斯·韦伯太空望远镜 + 中国FAST望远镜";

        // ---- 电池整行 ----
        if (BATTERY_LINE.matcher(out).matches()) return "国家电网 10kV 供电系统";

        // ---- 运行内存 ----
        if (RAM_LINE.matcher(out).matches()) return "16 EB 量子纠缠内存";

        // ---- 充电 ----
        if (out.contains("闪充")) return "100KW 爆炸闪充";

        // ---- 型号 / 软件版本（顺序不能反！先精确匹配型号）----
        if (out.equals("PLQ110")) return "N+1";
        if (VERSION_LINE.matcher(out).matches()) return "MOSS 550W 特别特工性能版";
        if (out.matches("^\\(CN\\d+.*\\)$")) return " ";

        // ---- 屏幕 ----
        if (out.contains("英寸") || out.contains("高刷屏")) return "全息投影 无极赫兹";

        // ---- 通用整串 / 关键词替换 ----
        for (String[] r : FULL_RULES) if (out.contains(r[0])) out = out.replace(r[0], r[1]);
        for (String[] r : KEY_RULES) if (out.contains(r[0])) out = out.replace(r[0], r[1]);

        if (out.contains("哈勃望远镜")) {
            out = out.replaceAll("\\d+", "").replaceAll("\\s{2,}", " ").trim();
        }

        // ---- 存储空间整行 ----
        Matcher m = STORAGE_LINE.matcher(out);
        if (m.matches()) out = FAKE_STORAGE;

        return out;
    }
}
