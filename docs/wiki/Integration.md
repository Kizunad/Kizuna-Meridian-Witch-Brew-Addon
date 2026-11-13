# 行药诀接入

书 `meridian_witch:brew_arts`，上卷 `guard` 含服药、敷药，中卷 `seed` 含掷引、泼引、噀催。
玩家使用 `/meridian technique learn @s meridian_witch:guard` 学习，再学中卷。
上卷游炁、中卷真炁，五式全程限速为原速 0.4。所有加成由目录声明，炼药处理器执行。

生存来源：两卷都声明了 `scroll_drops { entity_type: "minecraft:witch" chance: 0.25 }`，
功法库据此往原版女巫掉落表各加一池。玩家亲手杀死、且死者会这一整卷时按 0.25 掉一卷，不是玩家打死的不掉。
Threatening 学卷表让游炁、真炁女巫都会上卷，只有真炁会中卷。0.25 是占位概率。

## 动作与特效

**动作。**
- 服药、掷引、泼引、噀催在 0 ms 各有一条表现效果 `meridian_witch:witch_<剪辑>`，持续到收势结束（`UNTIL_RECOVERY_END`）。
  Human（玩家）和 Threatening（类人）去掉命名空间，播 PA 剪辑 `witch_imbibe`、`witch_lob_seed`、`witch_fling_seed`、`witch_spray_trigger`。
- 敷药有敷两臂、敷两腿两段剪辑，要看伤势才知道播哪段，所以目录不写表现效果，改在 0 ms 放一条处理器为
  `meridian_brewing:anoint_start` 的 `use_brew`。炼药在这一刻比两组伤势定组（平局取两臂），
  播 `witch_anoint_arms` 或 `witch_anoint_legs`，收势结束停；后面的揉搓声和药封都用这一组。
- 剪辑长度等于起手加收势；药用出去的那一帧（掷引叫 `release`，其余叫 `cast`）落在起手结束。

**特效。** 药用出去那一刻冒这种药本色的原版药水粒子（`entity_effect`），玩家和女巫放都有，由炼药发：
- 服药、敷药：腰间一圈淡粒子；
- 泼引：照目录的射程和扇角，在胸口高度铺开一片；
- 噀催：从嘴前顺着视线喷一道 3 m 的药雾；
- 掷引：出手处溅一小团，药瓶落地碎开另有原版喷溅药水的粒子。

## 卷内正文

五式的 `presentation.description` 就是卷轴上每式的正文，各一段老药婆的口吻，讲清哪一卷、什么境界能练、
认哪几样药、耗哪处经脉多少真元、起手和收势多久、挨打断不断、射程和范围。数字与目录同源：改了目录里的
费用、时长、射程，要同步改正文。正文是世界内文本，不写分数、倍率、按键、界面，也不夹英文。

## 验证

正式包没有 Java 入口。声音引用原版事件，字幕与名称提供中英文。
目录验证运行 `flock "/tmp/kizuna-gradle.lock" "./gradlew" --no-daemon test build`。
真实玩家施放与跨仓验证位于 Brewing 的 GameTest 装配。
掉卷由本仓 `gametest` 模块验证（`bash gradlew --no-daemon :gametest:runGametest -PgametestRunDir=<新目录>`，走完整装配）：
`WitchScrollDropGameTest` 用 Threatening 的生成命令生出真炁、游炁、凡人三只女巫，先查学卷表，
再各按原版女巫掉落表抽 200 次玩家击杀：会的卷各掉 25～75 次，不会的卷一次不掉，不是玩家打死的不掉。
`WitchCatalogTest` 另查两卷各只向女巫声明一项 0.25 的掉落；还查四式各挂一条从 0 ms 播到收势结束的剪辑，
敷药不挂剪辑、只在 0 ms 放定组那一条。动作和粒子的真实施放验证在 Brewing 的 `WitchPresentationGameTest`。
`WitchLoreTextTest` 查五式各有一段正文、彼此不同、不是旧占位，并逐段找机制说法和英文字母。
