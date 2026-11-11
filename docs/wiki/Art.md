# 行药诀美术

2026-11-11 补齐「养」「引」两卷的卷轴物品图标、阅读背景和五式战斗图标。此前启动日志报
`meridian_witch:guard_scroll`、`meridian_witch:seed_scroll` 缺少贴图、使用功法库通用卷轴图标，
战斗 UI 也因目录没有 `presentation.icon_id` 而不显示图标。

## 来源

- 全部主体由用户 srv 上的 CPA 生成：模型 `gpt-image-2.5-sunburst`，图片接口，`quality=high`、PNG、`style=none`。
- 调用脚本 `handoff/technique-art-20261028-work/cpa_generate.py` 读 `~/.config/kizuna/cpa-image.env`，日志不打印 key。
- 方形请求 1024×1024，CPA 实际返回 1254×1254 RGB；背景请求和返回都是 1536×1024。
- 原稿放仓内 `.local-art/{scroll,background,icon}/<名字>_v<版本>.png`，已加进 `.gitignore`，不进 JAR。
- 逐图提示词在 [art-prompts/](art-prompts/)；同一组的共用句（边框、装帧、禁止文字）每张都写全，只换主体。

## 成品

资源命名空间 `meridian_witch`，放 `fabric/src/main/resources/assets/meridian_witch/textures/`。路径由功法库约定：

- 卷轴物品 ID 是 `<卷>_scroll`；
- 阅读背景按卷路径取；
- 战斗图标按 `icon_id` 取 `gui/technique/<路径>.png`。

两卷是一套：

- 卷轴都是茄紫皮带、麻绳捆的纸卷，背景都是梅紫边、茄紫皮护角；
- 「养」卷偏疗养：直立的琥珀药瓶、干草药，背景是挂晒的草药、药瓶和药臼；
- 「引」卷偏投掷：卷身更细长，圆肚紫色溅射瓶、种荚，背景是溅射瓶、种荚和紫绿药雾。

| 卷 | 32×32 卷轴物品图 | 1536×1024 阅读背景 | 提示词 |
|---|---|---|---|
| 「养」 | [guard_scroll.png](../../fabric/src/main/resources/assets/meridian_witch/textures/item/guard_scroll.png) | [guard.png](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/scroll/guard.png) | [卷轴](art-prompts/guard_scroll_icon_prompt.txt)、[背景](art-prompts/guard_background_prompt.txt) |
| 「引」 | [seed_scroll.png](../../fabric/src/main/resources/assets/meridian_witch/textures/item/seed_scroll.png) | [seed.png](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/scroll/seed.png) | [卷轴](art-prompts/seed_scroll_icon_prompt.txt)、[背景](art-prompts/seed_background_prompt.txt) |

背景中间 x 18%–82%、y 12%–88% 是浅色纸面，给正文用。

五式共用一套边框：

- 近黑梅紫外圈；
- 暗苔绿内圈；
- 深茄紫底。

各式靠动作剪影区分。

| 式 | icon_id | 主体 | 32×32 | 64×64 | 提示词 |
|---|---|---|---|---|---|
| 服药 | `meridian_witch:imbibe` | 拔塞倾倒的琥珀药瓶 | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/imbibe.png) | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/imbibe_64.png) | [提示词](art-prompts/imbibe_technique_icon_prompt.txt) |
| 敷药 | `meridian_witch:anoint` | 药瓶滴药到掌心 | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/anoint.png) | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/anoint_64.png) | [提示词](art-prompts/anoint_technique_icon_prompt.txt) |
| 掷引 | `meridian_witch:lob_seed` | 溅射瓶沿高弧线飞出，落点紫色溅开 | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/lob_seed.png) | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/lob_seed_64.png) | [提示词](art-prompts/lob_seed_technique_icon_prompt.txt) |
| 泼引 | `meridian_witch:fling_seed` | 横甩药瓶，紫绿药滴扇形泼出 | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/fling_seed.png) | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/fling_seed_64.png) | [提示词](art-prompts/fling_seed_technique_icon_prompt.txt) |
| 噀催 | `meridian_witch:spray_trigger` | 兜帽侧脸喷出绿雾，雾里起黄色火星 | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/spray_trigger.png) | [PNG](../../fabric/src/main/resources/assets/meridian_witch/textures/gui/technique/spray_trigger_64.png) | [提示词](art-prompts/spray_trigger_technique_icon_prompt.txt) |

## 后处理

照 10-28 破门斩的做法，脚本是 [scripts/process_art.py](../../scripts/process_art.py)，成品全由原稿和脚本决定，不手修。

- **卷轴**：去品红底，判据是 R、B ≥ 130，G ≤ 140，且 R−G、B−G ≥ 60。然后依次：
  - 裁去透明边，最近邻缩到 30×30 以内；
  - 透明像素 RGB 归零后无抖动量化为 32 色；
  - 居中放进 32×32 RGBA，alpha 二值化为 0 或 255。
- **阅读背景**：保持 1536×1024 RGB。
- **战斗图标**：先最近邻缩成 64×64。与主要暗底 RGB 距离不超过 20 的颜色统一成本书底色 `#28012A`，再无抖动量化为 32 色。最后重画两圈边框：
  - 外圈 `#160216`，内圈 `#6C8C38`，颜色取自原稿实际画出的边框；
  - 64×64 每圈宽 2 像素；
  - 最近邻缩成 32×32 后每圈宽 1 像素。
- 主体不重画。盒式缩放对比过，更平滑但带糊边，不像像素画，所以和破门斩一样用最近邻。

重出某一张：

1. 改对应提示词；
2. 用 `handoff/art-20261111/art/run_cpa.sh <新版本> witch:<名字>:<icon|scroll|background>` 出原稿；
3. 再跑 `python3 scripts/process_art.py v1 <种类>/<名字>=<新版本>`，只换这一张。

当前成品全部来自 v1 原稿，复现命令是 `python3 scripts/process_art.py v1`；换过某张后把新参数更新在这里。

## 校验

`verification` 的 `WitchArtTest` 按目录逐项检查：

- 每卷有 32×32 卷轴物品图和 1536×1024 阅读背景；
- 每式的 `icon_id` 与式 ID 同名，32×32 和 64×64 图标都能解码。
