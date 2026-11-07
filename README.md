# SpringConfigConverter

IntelliJ 平台插件：Spring Boot 配置三格式互转（YAML / Properties / ENV），转换规则遵循 Spring Boot Relaxed Binding。

## 功能

- **选中文本转换**：编辑器中选中配置文本 → 右键菜单「Spring 配置转换」（结果复制到剪贴板），或 `Alt+Enter` 意图原地替换选区（支持撤销）
- **整文件转换**：右键文件（Project 视图 / 编辑器标签页）→ 在同目录生成目标格式新文件（如 `application.yml` → `application.properties`），重复执行会覆盖旧文件
- **格式自动识别**：按文件后缀识别 `.yml`/`.yaml`、`.properties`、`.env`，菜单项按源格式动态显隐（不显示与源相同的格式）
- **Spring 宽松绑定规则**：ENV 键大写、`.` 变 `_`、`-` 原样保留；properties 列表用 `key[0]` 索引，ENV 列表用 `_0_`；数值/布尔值转 YAML 时不带引号
- **快捷键**：`Alt+Shift+Y`（转 YAML）/ `Alt+Shift+P`（转 Properties）/ `Alt+Shift+E`（转 ENV）
- **通知反馈**：转换成功/失败均有 IDE 通知，失败时给出具体原因

## 构建

```bash
./gradlew buildPlugin
```

产物位于 `build/distributions/*.zip`。本机构建需 JDK 17+（`gradle.properties` 中 `org.gradle.java.home` 指向本机 JDK 21，换机器时请相应调整）。

## 安装

从 [Releases](https://github.com/edclol/SpringYamlToEnvConverter/releases) 下载 zip，然后 Settings → Plugins → ⚙️ → Install Plugin from Disk → 重启 IDE。

## 版本管理与发布

版本以 **标签** 管理：日常开发在 `main`，发版时打 `vX.Y.Z` 标签（需与 `gradle.properties` 的 `version` 一致），推送后 GitHub Actions 自动构建并发布 Release：

```bash
git tag v2.0.0
git push origin v2.0.0
```

需要给旧版本出补丁时，从对应标签拉出分支修复（如 `git checkout v2.0.0 -b release/2.x`），修完打新标签即可复用同一流水线。工作流定义见 [.github/workflows/release.yml](.github/workflows/release.yml)。

## 技术要点

- YAML 解析/生成使用 IntelliJ 平台自带的 SnakeYAML
- 转换核心：统一解析为嵌套 Map 中间表示，再按目标格式渲染；连续数字键自动还原为列表
- 最低兼容 IntelliJ Platform 2025.3（构建基于 `intellij-platform-gradle-plugin`）
