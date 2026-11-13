# 行药诀接入

书 `meridian_witch:brew_arts`，上卷 `guard` 含服药、敷药，中卷 `seed` 含掷引、泼引、噀催。
玩家使用 `/meridian technique learn @s meridian_witch:guard` 学习，再学中卷。
上卷游炁、中卷真炁，五式全程限速为原速 0.4。所有加成由目录声明，炼药处理器执行。

生存来源：两卷都声明了 `scroll_drops { entity_type: "minecraft:witch" chance: 0.25 }`，
功法库据此往原版女巫掉落表各加一池。玩家亲手杀死、且死者会这一整卷时按 0.25 掉一卷，不是玩家打死的不掉。
Threatening 学卷表让游炁、真炁女巫都会上卷，只有真炁会中卷。0.25 是占位概率。

正式包没有 Java 入口。声音引用原版事件，字幕与名称提供中英文。
目录验证运行 `flock "/tmp/kizuna-gradle.lock" "./gradlew" --no-daemon test build`。
真实玩家施放与跨仓验证位于 Brewing 的 GameTest 装配。
掉卷由本仓 `gametest` 模块验证（`bash gradlew --no-daemon :gametest:runGametest -PgametestRunDir=<新目录>`，走完整装配）：
`WitchScrollDropGameTest` 用 Threatening 的生成命令生出真炁、游炁、凡人三只女巫，先查学卷表，
再各按原版女巫掉落表抽 200 次玩家击杀：会的卷各掉 25～75 次，不会的卷一次不掉，不是玩家打死的不掉。
`WitchCatalogTest` 另查两卷各只向女巫声明一项 0.25 的掉落。
