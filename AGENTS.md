# 服集 FitLog — Agent 接手入口

- 当前开发基线是默认分支 main，产品版本 v0.4.8（versionCode 14）。V0.3 是历史版本。
- AI 配色（v0.4.8 起）：DeepSeek API 可选接入。关键文件：data/DeepSeekClient.kt（非流式 + SSE 流式 chatStream）、data/AiRepository.kt（prompt/解析/衣橱摘要）、data/ColorKnowledge.kt + assets/color_knowledge.json（内置配色知识库，主题关键词检索）、ui/screens/ChatScreen.kt（流式聊天 + 思考指示器）。API key 存 SharedPreferences（AiSettings），隐私说明文案已改为「可选联网、仅发文字信息」。无 key/断网时回退规则建议与硬编码色板，不得破坏离线可用性。
- 开始工作先读 README.md 和 docs/FitLog-Agent-Handoff.md，检查 git status、当前分支以及远端 main 的提交；保护未提交修改，不要硬重置。
- 产品重点：先决定穿某一件，再围绕它搭配；保留尺码/身体数据、图片、成套搭配。当前不做每日穿搭日记。
- UI：鼠尾草绿、简洁留白；名称「服集 FitLog」，图标为绿底米白衣架与 F。
- 预览源码：docs/preview/Fuji-FitLog-Preview.html，是独立 HTML 模拟，需与 Android 界面分别同步维护。在线预览链接见 README。
- 旧版无法备份的问题用户已确认解决；不要重复修复。真实手机安装版本、签名及迁移完成情况仍需以实际证据核实。
- applicationId、数据库、图片路径和备份格式的修改必须保护现有数据。预览包与旧版私有数据独立，不要要求卸载或清除数据来迁移。
- 依据修改运行必要验证；不要把模拟测试说成真机测试。当前构建入口：./gradlew :app:testDebugUnitTest :app:assemblePreview。
- 阶段结束时更新交接文档、版本/产物位置和验证状态；用户要求更新仓库时明确默认分支是否已经更新，不要仅留在未合并分支。
