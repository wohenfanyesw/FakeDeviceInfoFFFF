package com.dsh.fakedevice;

import android.app.ActivityManager;
import android.widget.TextView;

import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 「自欺欺人」模块：只改 com.android.settings（关于本机）里显示的文字与数值。
 * 纯运行时 hook，不改任何分区、不写系统文件，卸载模块即完全还原。
 */
public class MainHook implements IXposedHookLoadPackage {

    /** 内存伪装：102400 TB（单位字节，long 可容纳） */
    private static final long FAKE_TOTAL_MEM = 112589990684262400L;

    /** 存储伪装文本（byte 级超出 long 上限，所以直接替换显示文本） */
    private static final String FAKE_STORAGE = "1145141919810 TB";

    /** 文本替换规则：原文关键字 -> 替换值 */
    private static final String[][] TEXT_RULES = {
            {"骁龙", "AMD Ryzen 9 9950X3D"},
            {"至尊版移动平台", "AMD Ryzen 9 9950X3D"},
            {"潮汐引擎", "中国嫦娥空间站"},
            {"风驰游戏内核", "NVIDIA GEFORCE"},
            {"电竞三芯", "AMD RADEON"},
            {"极速高刷", "INTEL IRIS XE ARC"},
            {"冰川电池", "国家电网 10kV 供电系统"},
            {"万像素", "哈勃望远镜"},
    };

    private static final Pattern STORAGE_PATTERN =
            Pattern.compile(".*\\d+(\\.\\d+)?\\s*(GB|TB|MB|KB)\\s*/\\s*\\d+(\\.\\d+)?\\s*(GB|TB|MB|KB).*",
                    Pattern.CASE_INSENSITIVE);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) {
        if (!"com.android.settings".equals(lp.packageName)) return;
        XposedBridge.log("[FakeDeviceInfo] hooked " + lp.packageName);

        // 1) 所有 TextView 文本落地前替换
        XposedHelpers.findAndHookMethod(TextView.class, "setText", CharSequence.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                CharSequence cs = (CharSequence) param.args[0];
                if (cs == null) return;
                String src = cs.toString();
                String dst = transform(src);
                if (!dst.equals(src)) param.args[0] = dst;
            }
        });

        // 2) 运行内存：hook 后改写 totalMem（102400 TB）
        XposedHelpers.findAndHookMethod(ActivityManager.class, "getMemoryInfo",
                ActivityManager.MemoryInfo.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        ActivityManager.MemoryInfo mi = (ActivityManager.MemoryInfo) param.args[0];
                        if (mi != null) mi.totalMem = FAKE_TOTAL_MEM;
                    }
                });
    }

    private static String transform(String s) {
        String out = s;
        for (String[] rule : TEXT_RULES) {
            if (out.contains(rule[0])) out = out.replace(rule[0], rule[1]);
        }
        // 摄像头行：保留文字，去掉残留数字
        if (out.contains("哈勃望远镜")) out = out.replaceAll("\\d+", "").replaceAll("\\s{2,}", " ").trim();
        // 存储空间整行替换
        if (STORAGE_PATTERN.matcher(out).matches()) out = FAKE_STORAGE + " / " + FAKE_STORAGE;
        return out;
    }
}
