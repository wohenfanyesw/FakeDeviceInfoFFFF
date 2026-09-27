# 自欺欺人 · 设备信息模块（LSPosed）

把「设置 → 关于本机」里的显示改成自定义值（纯运行时 hook，不碰分区）。

## 一键出包（GitHub Actions）

1. 新建一个 GitHub 仓库（公开/私有都行）
2. 把本文件夹里的**全部文件**上传上去（保持目录结构，`.github/workflows/build.yml` 必须存在）
3. 进入仓库 **Actions** 标签 → 选择 `Build Module APK` → 点 **Run workflow**
4. 等 1~2 分钟编译完成 → 在该次运行页面底部下载 **Artifacts → FakeDeviceInfo-apk**
5. 解压得到 `app-debug.apk`

## 安装启用

1. 安装 APK（装完桌面可能没有图标，正常现象）
2. 打开 **LSPosed** 管理器 → 模块 → 启用「自欺欺人 · 设备信息」
3. 作用域勾选 **设置 (com.android.settings)**（模块已声明 scope，会自动勾上）
4. 强制停止「设置」或重启手机 → 打开「关于本机」查看效果

## 当前替换规则（改 `MainHook.java` 即可自定义）

| 原文 | 替换为 |
|---|---|
| 骁龙 / 至尊版移动平台 | AMD Ryzen 9 9950X3D |
| 潮汐引擎 | 中国嫦娥空间站 |
| 风驰游戏内核 | NVIDIA GEFORCE |
| 电竞三芯 | AMD RADEON |
| 极速高刷 | INTEL IRIS XE ARC |
| 冰川电池 | 国家电网 10kV 供电系统 |
| 万像素 | 哈勃望远镜（并去掉残留数字） |
| 运行内存 | 102400 TB |
| 存储空间 x/y | 1145141919810 TB / 1145141919810 TB |

## 卸载还原

LSPosed 里关掉模块（或卸载 APK）→ 重启，一切恢复原样。本模块不修改任何系统文件。
