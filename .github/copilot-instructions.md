# MindustryX 开发指南

## 项目初始化

**克隆项目（含子模块）：**
```bash
git clone --recursive https://github.com/TinyLake/MindustryX.git
cd MindustryX
```

**若已克隆但未初始化子模块：**
```bash
git submodule update --init --depth=10
```

**应用 Patch 文件（必须在编译前执行）：**
```bash
./scripts/applyPatches.sh
```

## 编译

本项目基于 **Gradle 8.12** 构建，要求 **JDK 17**。

**切换到 work 目录后执行编译：**
```bash
cd work
gradle --parallel desktop:dist server:dist core:genLoaderModAll android:assembleRelease
```

**编译产物路径：**
- 桌面端：`work/desktop/build/libs/Mindustry.jar`
- 服务端：`work/server/build/libs/server-release.jar`
- Loader Mod（dex）：`work/core/build/distributions/MindustryX.loader.dex.jar`
- Android APK：`work/android/build/outputs/apk/release/android-release.apk`

**指定版本号编译：**
```bash
cd work
gradle -Pbuildversion=<版本号> --parallel desktop:dist
```

## 目录结构

```
MindustryX/
├── .github/
│   ├── workflows/
│   │   ├── build.yml         # CI：每次 push 触发构建并发布预览版
│   │   └── release.yml       # CI：手动触发发布正式版
│   └── actions/
│       └── clearOldAssets.ts # 清理旧 Release 资源的脚本
├── Arc/                      # 子模块：上游 Arc 引擎（已应用 patches/arc 补丁）
├── work/                     # 子模块：上游 Mindustry 源码（已应用 patches 补丁）
├── patches/
│   ├── arc/                  # 作用于 Arc/ 的补丁文件
│   ├── picked/               # 作用于 work/ 的精选补丁（上游 cherry-pick）
│   └── client/               # 作用于 work/ 的 MindustryX 客户端功能补丁
├── src/
│   ├── arc/                  # Arc 相关的补充源码
│   └── mindustryX/           # MindustryX 核心源码
│       ├── Hooks.java         # 注入钩子
│       ├── MindustryXApi.java # 对外 API
│       ├── VarsX.kt           # 全局变量扩展
│       ├── events/            # 事件相关
│       ├── features/          # 各功能模块
│       └── loader/            # Loader Mod 入口
├── assets/                   # 游戏资产（图片、配置、mod.hjson 等）
├── buildPlugins/             # Gradle 构建插件
├── scripts/
│   ├── applyPatches.sh       # 将 patches/ 应用到 Arc/ 和 work/
│   ├── genPatches.sh         # 从 Arc/ 和 work/ 生成 patches/
│   └── updateUpstream.sh     # 更新上游 submodule 并重建补丁
├── .gitmodules               # 子模块配置（Arc 和 work）
└── README.md
```

## 开发工作流

1. **初始化**：克隆项目并执行 `./scripts/applyPatches.sh`
2. **开发**：在 `work/` 目录下修改代码并 `git commit`
3. **生成补丁**：在项目根目录执行 `./scripts/genPatches.sh`，自动更新 `patches/` 目录
4. **提交**：在项目根目录提交 `patches/` 目录的变更，然后创建 PR

> **注意**：直接修改 `work/` 或 `Arc/` 目录中的文件不会被追踪到主仓库，所有变更必须通过 patch 文件管理。
