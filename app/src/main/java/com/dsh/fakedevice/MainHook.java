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

/** 「自欺欺人」模块：完整规则只作用于「设置」；手机管家只做游戏架构三条最小替换。 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String PKG_SETTINGS = "com.android.settings";
    private static final String PKG_PHONEMGR = "com.coloros.phonemanager";

    /** true = 当前进程只做最小替换（非设置进程） */
    private static boolean limited = false;

    private static final String FAKE_STORAGE = "114514 ZB / TREE(3) YB / ∞ (阿列夫一) ZB";

    private static final String[][] FULL_RULES = {
            {"骁龙®8至尊版移动平台", "AMD Ryzen 9 9950X3D"},
            {"骁龙®8至尊版", "AMD Ryzen 9 9950X3D"},
            {"骁龙8至尊版移动平台", "AMD Ryzen 9 9950X3D"},
            {"高通骁龙8至尊版", "AMD Ryzen 9 9950X3D"},
            {"一加 Ace 6", "ONEPLUS NEVERSETTLE"},
    };

    private static final String[][] KEY_RULES = {
            {"潮汐引擎", "中国嫦娥空间站"},
            {"风驰游戏内核", "NVIDIA GEFORCE"},
            {"电竞三芯", "AMD RADEON"},
            {"极速高刷", "INTEL IRIS XE ARC"},
            {"冰川电池", " "},
            {"万像素", "哈勃望远镜"},
    };

    /** 最小模式只替换这三组（游戏架构） */
    private static final String[][] MINI_RULES = {
            {"风驰游戏内核", "NVIDIA GEFORCE"},
            {"电竞三芯", "AMD RADEON"},
            {"极速高刷", "INTEL IRIS XE ARC"},
    };

    private static final Pattern BATTERY_LINE =
            Pattern.compile(".*mAh.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAM_LINE =
            Pattern.compile("^\\s*16(?:\\.0+)?\\s*GB\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORAGE_LINE = Pattern.compile(
            "^(.*?)\\s*/\\s*\\d+(?:\\.\\d+)?\\s*(?:GB|TB|MB|KB)\\s*$", Pattern.CASE_INSENSITIVE);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        boolean isSettings = PKG_SETTINGS.equals(lp.packageName);
        boolean isPhoneMgr = PKG_PHONEMGR.equals(lp.packageName);
        if (!isSettings && !isPhoneMgr) return;

        limited = !isSettings; // 手机管家 -> 最小替换模式
        XposedBridge.log("[FakeDeviceInfo] loaded in " + lp.packageName + " limited=" + limited);

        hookRes("getString", int.class);
        hookRes("getText", int.class);
        hookRes("getString", int.class, Object[].class);

        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) { replaceArg(p, 0); }
        });
        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class,
                TextView.BufferType.class, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) { replaceArg(p, 0); }
                });
    }

    private static void replaceArg(XC_MethodHook.MethodHookParam p, int idx) {
        Object a = p.args[idx];
        if (a == null) return;
        String src = a.toString();
        String dst = transform(src);
        if (!dst.equals(src)) p.args[idx] = dst;
    }

    private static void hookRes(String name, Class<?>... args) {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, name, args, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
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
        String out = s.replace('\u00A0', ' ').replace('\u3000', ' ').trim();

        // ---- 非「设置」进程：只做游戏架构三条最小替换 ----
        if (limited) {
            for (String[] r : MINI_RULES) if (out.contains(r[0])) out = out.replace(r[0], r[1]);
            return out;
        }

        // ---- 摄像头两行 ----
        if (out.startsWith("前置")) return "前置 哈勃望远镜";
        if (out.startsWith("后置")) return "后置 詹姆斯·韦伯太空望远镜 + 中国FAST望远镜";

        // ---- 电池 ----
        if (BATTERY_LINE.matcher(out).matches()) return "国家电网 10kV 供电系统";

        // ---- 运行内存 ----
        if (RAM_LINE.matcher(out).matches()) return "16 EB 量子纠缠内存";

        // ---- 充电 ----
        if (out.contains("闪充")) return "100KW 爆炸闪充";

        // ---- 型号 / 软件版本 ----
        if (out.equals("PLQ110")) return "N+1";
        if (out.contains("PLQ110")) return "MOSS 550W 特别特工性能版";
        if (out.matches("^\\(CN\\d+[\\s\\S]*\\)$")) return " ";

        // ---- 屏幕 ----
        if (out.contains("英寸") || out.contains("高刷屏")) return "全息投影 无极赫兹";

        // ---- 通用替换 ----
        for (String[] r : FULL_RULES) if (out.contains(r[0])) out = out.replace(r[0], r[1]);
        for (String[] r : KEY_RULES) if (out.contains(r[0])) out = out.replace(r[0], r[1]);

        if (out.contains("哈勃望远镜")) {
            out = out.replaceAll("\\d+", "").replaceAll("\\s{2,}", " ").trim();
        }

        Matcher m = STORAGE_LINE.matcher(out);
        if (m.matches()) out = FAKE_STORAGE;

        return out;
    }
}
