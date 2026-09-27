package com.dsh.fakedevice;

import android.app.ActivityManager;
import android.content.res.Resources;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 「自欺欺人」模块：只改 com.android.settings（关于本机）显示。
 * 纯运行时 hook，不改分区、不写系统文件，关闭模块即还原。
 */
public class MainHook implements IXposedHookLoadPackage {

    /** 运行内存伪装：102400 TB（字节） */
    private static final long FAKE_TOTAL_MEM = 112589990684262400L;
    /** 存储总容量伪装文本 */
    private static final String FAKE_TOTAL_STORAGE = "1145141919810 TB";

    /** 整串替换（顺序在前，优先匹配长串，避免拼出怪东西） */
    private static final String[][] FULL_RULES = {
            {"骁龙®8至尊版移动平台", "AMD Ryzen 9 9950X3D"},
            {"骁龙®8至尊版", "AMD Ryzen 9 9950X3D"},
            {"骁龙8至尊版移动平台", "AMD Ryzen 9 9950X3D"},
            {"高通骁龙8至尊版", "AMD Ryzen 9 9950X3D"},
            {"一加 Ace 6", "ONEPLUS NEVERSETTLE"},
            {"7800 mAh（典型值）", "国家电网 10kV 供电系统"},
            {"7800 mAh(典型值)", "国家电网 10kV 供电系统"},
            {"7800mAh", "国家电网 10kV 供电系统"},
            {"16.0 GB", "102400 TB"},
    };

    /** 关键词替换 */
    private static final String[][] KEY_RULES = {
            {"潮汐引擎", "中国嫦娥空间站"},
            {"风驰游戏内核", "NVIDIA GEFORCE"},
            {"电竞三芯", "AMD RADEON"},
            {"极速高刷", "INTEL IRIS XE ARC"},
            {"冰川电池", " "},
            {"万像素", "哈勃望远镜"},
    };

    /** 存储卡片：把「已用 / 总容量」的“总容量”换成假值，保留真实已用 */
    private static final Pattern STORAGE = Pattern.compile(
            "^(.*?)\\s*/\\s*(\\d+(?:\\.\\d+)?)\\s*(GB|TB|MB|KB)\\s*$", Pattern.CASE_INSENSITIVE);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.android.settings".equals(lp.packageName)) return;
        XposedBridge.log("[FakeDeviceInfo] loaded in " + lp.packageName);

        // 1) 资源文本（覆盖 setText(R.string.xxx) 这条路径）
        hookRes("getString", int.class);
        hookRes("getText", int.class);
        hookRes("getString", int.class, Object[].class);

        // 2) 直接设置 CharSequence 的路径
        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                CharSequence cs = (CharSequence) p.args[0];
                if (cs == null) return;
                String src = cs.toString();
                String dst = transform(src);
                if (!dst.equals(src)) p.args[0] = dst;
            }
        });

        // 3) 运行内存
        XposedHelpers.findAndHookMethod(ActivityManager.class, "getMemoryInfo",
                ActivityManager.MemoryInfo.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        ActivityManager.MemoryInfo mi = (ActivityManager.MemoryInfo) p.args[0];
                        if (mi != null) mi.totalMem = FAKE_TOTAL_MEM;
                    }
                });
    }

    private static void hookRes(String name, Class<?>... args) {
        try {
            XposedHelpers.findAndHookMethod(Resources.class, name, args, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    Object r = p.getResult();
                    if (r instanceof String) {
                        String s = (String) r;
                        String t = transform(s);
                        if (!t.equals(s)) p.setResult(t);
                    }
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("[FakeDeviceInfo] hookRes " + name + " failed: " + t);
        }
    }

    private static String transform(String s) {
        String out = s;
        for (String[] r : FULL_RULES) if (out.contains(r[0])) out = out.replace(r[0], r[1]);
        for (String[] r : KEY_RULES)  if (out.contains(r[0])) out = out.replace(r[0], r[1]);
        if (out.contains("哈勃望远镜")) out = out.replaceAll("\\d+", "").replaceAll("\\s{2,}", " ").trim();
        Matcher m = STORAGE.matcher(out.trim());
        if (m.matches()) out = m.group(1) + " / " + FAKE_TOTAL_STORAGE;
        return out;
    }
}
