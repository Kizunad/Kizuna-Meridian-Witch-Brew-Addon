# 仓库协作约定

- 简体中文沟通、文档和函数内注释；遵守 `~/.codex/AGENTS.md` 的代码形状规则。
- 正式包是纯数据附属：`shared/` 保存唯一 textproto，`fabric/` 保存声音、语言与贴图，无 Java 入口。
- 世界执行归炼药，通用施放契约归功法库，不复制其实现或协议。
- 文档来源为 `docs/wiki/`。Gradle 使用 `flock /tmp/kizuna-gradle.lock` 串行并带 `--no-daemon`。
- 交付验证 build、目录与真实玩家施放链；未经授权不提交、不推送、不创建远端。
