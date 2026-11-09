# 行药诀接入

书 `meridian_witch:brew_arts`，上卷 `guard` 含服药、敷药，中卷 `seed` 含掷引、泼引、噀催。
玩家使用 `/meridian technique learn @s meridian_witch:guard` 学习，再学中卷。
上卷游炁、中卷真炁，五式全程限速为原速 0.4。所有加成由目录声明，炼药处理器执行。

正式包没有 Java 入口。声音引用原版事件，字幕与名称提供中英文。
目录验证运行 `flock "/tmp/kizuna-gradle.lock" "./gradlew" --no-daemon test build`。
真实玩家施放与跨仓验证位于 Brewing 的 GameTest 装配。
