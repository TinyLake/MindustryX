# AGENTS.md - MindustryX 仓库说明

## 仓库结构（关键目录）
```text
MindustryX/
|-- Arc/                # Arc 上游展开仓库，arc 补丁应用目标
|-- work/               # Mindustry 上游展开仓库，真实 Gradle 构建入口
|-- patches/
|   |-- arc/            # Arc 补丁输出
|   |-- picked/         # 前置挑选补丁
|   \-- client/         # 主客户端补丁
|-- scripts/
|   |-- applyPatches.sh # 从补丁链展开到 Arc/work
|   \-- genPatches.sh   # 从 Arc/work 重新生成补丁
|-- src/                # 补丁层源码/映射源码，不要把它当成唯一构建事实来源
|-- assets/             # 资源、图标、bundle
|-- buildPlugins/       # Gradle 构建逻辑
\-- AGENTS.md
```

## 工作模型
- 这是一个 patch-first 仓库。真正参与 Gradle 构建和运行验证的是 `work/` 与 `Arc/`，不是根目录 `patches/` 文本本身。
- 功能修改默认应在展开后的源码里完成，再回到根目录重新生成 patch；不要把 `patches/*.patch` 当作主要编辑对象，除非任务明确要求只改 patch 元数据或上下文说明。
- 开始改动前先确认本次任务落在哪一层：`work/`、`Arc/`、根目录资源、补丁生成脚本，还是 CI/发布配置；不要混层修改。
- 根目录仓库和 `work/`/`Arc/` 是不同 Git 上下文。执行 `git`、`gh`、提交、推送、看 diff 之前，先确认当前目录、当前 worktree、当前 branch。
- 如果遇到 `safe.directory`、子模块 revision 缺失、Gradle 依赖下载失败、GitHub API/CLI 瞬时 `EOF`，优先把它视为环境/工具链问题，不要直接归因于代码逻辑。

## 构建与验证
- Gradle 构建要求 JDK 17 及以上；但项目多数 Java 模块仍以 Java 8 目标发布，新增代码不要随意引入高版本 Java API。
- 首次展开补丁链或需要重置展开源码时，在仓库根目录运行：

```bash
bash ./scripts/applyPatches.sh
```

- 常用验证应从 `work/` 目录执行，按任务选择最小命令：

```powershell
cd work
./gradlew.bat :desktop:dist -x test
./gradlew.bat :tests:test
./gradlew.bat :server:dist
./gradlew.bat :core:genLoaderModAll
```

- 完整 CI 风格构建会组合执行 `desktop:dist server:dist core:genLoaderModAll android:assembleRelease`；只有在确实需要覆盖多产物时才跑全量。
- 修改 `work/` 或 `Arc/` 后，需要回到根目录重新生成补丁：

```bash
bash ./scripts/genPatches.sh
```

- 如果验证被网络、TLS、远端仓库或本地环境阻塞，交付时要明确区分“代码未通过”与“环境导致无法完成验证”。
- 临时产物路径（如手工回写 `MindustryX-temp.jar`）只能用于快速验证，不能代替 patch 再生成与正式构建链路。

## 改动约束
- 保持改动最小化、局部化，不要借修一个点顺手做风格化重构或无关清理。
- 优先复用项目现有抽象、数据模型和入口；不要为了局部整洁额外引入 helper、转发层或重复数据结构。
- 初始化副作用要显式，避免在构造函数、`init` 或隐蔽路径里偷偷注册 hook、改全局状态，除非这是当前模块既有模式且本次改动必须延续。
- 数据归一化、默认值修正、兼容处理应尽量放在数据拥有者附近，而不是依赖外部“额外调用一次”来兜底。
- 涉及消息格式、协议、兼容逻辑时，要显式划清版本边界，默认行为优先保持稳定。
- Review 修改以“行为正确 + 改动边界合理”为目标，不要只追求“现在能跑”。

## UI 与国际化
- UI 适配优先复用现有 `Table` 和项目已有布局能力，不要依赖硬编码宽高阈值或“挤不下就换行”的补丁式布局。
- 信息展示优先使用结构化组件，不要把多段信息、颜色、状态和换行需求都堆进字符串拼接。
- 不要用字符串拼接生成富文本或颜色标记文本，例如 `"[accent]" + text + "[]"`；优先拆成独立 `Label`/`Cell`，或直接对组件设置颜色、样式和布局。
- 不要滥用 `i(...)` 内联国际化 helper。正式、长期存在、可复用的 UI 文案优先写入 bundle；`i(...)` 更适合少量局部文本或确实不值得沉淀 bundle 的场景。
- 含变量的文案优先整句写成 bundle 项并使用占位符格式化，例如把“回放文件不存在：{0}”作为一个完整词条，而不是先翻译半句再拼接文件名、标点或状态值。
- 列表项、卡片项或复杂子区域应有清晰的局部组件边界；有抽象价值再提炼，没有价值的包裹层不要保留。
- 功能入口要克制。已有明确入口时，不要额外增加快捷按钮、重复设置入口或平行 UI 路径。
- 用户可见文案必须走 bundle/资源文件，不要硬编码。修改界面时，通常要同时检查布局代码和对应的 bundle 词条。
- 同一组件相关的 i18n 文案应尽量集中维护，便于 review、补翻译和后续重命名。

## Review 与收尾流程
- GitHub review comment 可能锚定旧 diff 位置；处理前先对照当前实现，不要机械按旧行号修改。
- `gh` 或 GitHub API 的瞬时失败应先重试，再判断是否真的是权限或逻辑问题。
- 收尾操作必须串行，不要并发执行 `build`、产物更新、`commit`、`push`、review 回复、resolve thread。
- 交付时如果同时涉及展开源码与生成补丁，应同时检查 `work/`/`Arc/` 中的实际修改点，以及根目录 `patches/` 的生成结果。

## 命令约定
- 在 Windows 上优先使用 PowerShell 7（`pwsh`）执行日常命令；需要运行仓库脚本时显式调用 `bash`。
