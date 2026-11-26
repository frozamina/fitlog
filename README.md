# 服集 FitLog

**当前开发版本：v0.4.15（versionCode 21） · 最新源码在默认分支 main。**

以一件想穿的衣服为起点，从自己的衣橱中找到搭配。原生 Android、本地存储、无需账号，采用鼠尾草绿 Sage & Ivory 界面。

## 接手开发先看这里

1. [Agent 入口](AGENTS.md)
2. [完整交接文档](docs/FitLog-Agent-Handoff.md)
3. [整合版功能与数据说明](docs/整合版说明.md)
4. [可交互 HTML 源码](docs/preview/Fuji-FitLog-Preview.html)（下载文件后可直接在浏览器打开）
5. [在线安卓交互预览](https://fuji-fitlog-preview-261124.xifeng335.chatgpt.site)

原 V0.3 文档和标签是历史资料，不能据此判断最新功能。若 Workbuddy 仍显示 V0.3，先检查其本地 checkout、分支、标签或缓存。不要覆盖未提交工作。

## 当前功能

- 统一衣物资料：尺码、自定义属性、照片、主色、图案、版型与合身评价。
- 衣橱搜索、分类与收藏；照片和尺寸可逐步补充。
- 从选定单品出发搭配，替换/添加衣物，保存、修改或另存整套搭配。
- 配色灵感和离线规则建议；AI 配色（DeepSeek）可选开启：灵感页 AI 生成色板、配色问答聊天（SSE 流式 + 内置配色知识库）。
- 身体数据历史、属性模板、尺码对照、ZIP 备份导入/导出。
- 不包含每日穿搭日记，搭配不绑定日期。

## 预览与安装

最新预览 APK 为 `FitLog-v0.4.15.apk`（由助手在本机构建后交付到用户 Downloads，不发布 GitHub Release）。

预览包为 `com.fitlog.app.preview`，与原版 `com.fitlog.app` 独立共存。v0.4.15 预览 APK 与此前预览 APK 的签名一致，可直接覆盖安装。不要卸载旧应用来迁移数据；旧数据通过备份导入，导入采用追加方式。

网页是独立的 UI 交互模拟，使用示例数据并保存在浏览器；不会读取手机数据，原生备份相关操作并未在网页实现。修改 Android 时需要同步维护网页，GitHub 更新不会自动部署在线站点。

## 构建与验证

JDK 17、Android SDK Platform 34、Build Tools 34.0.0，支持 Android 8.0+（API 26）。

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assemblePreview
```

预览 APK 输出到 `app/build/outputs/apk/preview/`。开发前配置本机 SDK 路径，勿提交 local.properties 或签名密钥。最近 v0.4.15 的测试与构建通过；AI 功能需用户在真机配置 DeepSeek API key 后验证联网调用。

## 后续维护

完成阶段性工作后，同步更新本 README 的版本、本项目交接文档及预览；记录实际提交和验证结果。签名、数据库和图片迁移需要实机验证。旧的 PRD 与蓝色 UI 展示位于 docs，仅供历史参考。
