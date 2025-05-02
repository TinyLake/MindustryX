<img src=assets/icon.png height="64"> <img src=.github/assets/x-logo.png height="64">


![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/TinyLake/MindustryX/build.yml?label=Building)  ![GitHub Release](https://img.shields.io/github/v/release/TinyLake/MindustryX?label=Latest%20Version&labelColor=blue&color=green&link=https%3A%2F%2Fgithub.com%2FTinyLake%2FMindustryX%2Freleases)  ![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/TinyLake/MindustryX/total?label=Downloads)

## MindustryX

- 新一代Mindustry分支，目标是打造一个 **高质量、更加开放** 的第三方生态。
- 目前MDTX生态包括：客户端功能与优化，服务端优化，API拓展。

### 版本号规则
前三位是发布日期，第四位是构件号。后面是编译分支

比如 `2024.05.25.238-client-wz` 对应 `{date}.{code}-{branch}`, `code` 是每个分支的编译序列码

### 发布类型
* apk为安卓版
* jar为桌面版
* loader.jar为Mod版 **[推荐]**  
(可用原版启动，已支持pc,steam,android全平台)

### 客户端功能
为了减少迁移不适，客户端涵盖了 **绝大部分学术端功能** ，并进行大量整理和优化。 除此之外已有大量MDTX原创功能与性能优化。

详见 [MDTX wiki](https://github.com/TinyLake/MindustryX/wiki) 或者查阅 **[Patches](./patches)**

### 安装方式
在 [Releases](https://github.com/TinyLake/MindustryX/releases) 中下载对应平台的MDTX

**Loader 需要作为mod导入游戏**

### 贡献代码
1. 使用 `git clone --recursive https://github.com/TinyLake/MindustryX.git` 或者在 `clone` 后，执行 `git submodule init` 初始化mdt模块
2. cd [`work/`](work) 并运行 `../scripts/applyPatches`
3. 在 [`work/`](work) 中提交你的代码
4. 用 `../scripts/genPatches.sh` 生成 patch 文件.
5. 在 MDTX 根目录里提交 patch 文件

有开发能力的可私聊WZ加入开发群

### MindustryX Client
* More Api for `mods`
* Better performance
* Better Quality-of-Life(QoL)
* Compatible with official client
* More aggressive bug fixing and experience new feature.(Release more frequently than the upstream)

### Version rule
Like `2024.05.25.238-client-wz` means `{date}.{code}-{branch}`, `code` increment each ci build.

### Features
See `./patches/`.

## Contribution
1. cd `work/` and execute `../scripts/applyPatches`
2. commit your feature in `work/`, then `../scripts/genPatches.sh` and commit in root.