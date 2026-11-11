#!/usr/bin/env python3
"""把 .local-art/ 里的 CPA 原稿缩放整理成行药诀的卷轴物品图、阅读背景和战斗图标。

做法照破门斩：卷轴去品红底后缩到 30×30 放进 32×32 透明画布；战斗图标先缩成 64×64，
统一暗底色和两圈边框，再缩成 32×32；阅读背景保持 1536×1024。详见 docs/wiki/Art.md。

用法：python3 scripts/process_art.py [默认版本，缺省 v1] [种类/名字=版本 ...]，种类是 scroll、background、icon。
例：python3 scripts/process_art.py v1 icon/spray_trigger=v2 只把噀催图标换成 v2 原稿。
"""
import sys
from collections import Counter
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / ".local-art"
TEXTURES = ROOT / "fabric/src/main/resources/assets/meridian_witch/textures"
VOLUMES = ("guard", "seed")
TECHNIQUES = ("imbibe", "anoint", "lob_seed", "fling_seed", "spray_trigger")
# 五式共用的边框配色，取自原稿实际画出的颜色：深茄紫底、近黑梅紫外圈、暗苔绿内圈。
ICON_BACKGROUND = (40, 1, 42)
OUTER_RIM = (22, 2, 22)
INNER_RIM = (108, 140, 56)

SCROLL_SIZE = 32
SCROLL_INSET = 30
ICON_SIZE = 32
ICON_LARGE = 64
ICON_SAMPLE_MARGIN = 4
MAGENTA_RED_MIN = 130
MAGENTA_BLUE_MIN = 130
MAGENTA_GREEN_MAX = 140
MAGENTA_CHANNEL_GAP = 60
ALPHA_THRESHOLD = 128
OPAQUE = 255
PALETTE_COLORS = 32
BACKGROUND_MAX_CHANNEL = 70
BACKGROUND_COLOR_DISTANCE = 20
LARGE_RIM_WIDTH = 2
BACKGROUND_SIZE = (1536, 1024)


def raw(kind, name, versions):
    path = RAW / kind / f"{name}_{versions.get(f'{kind}/{name}', versions[None])}.png"
    if not path.is_file():
        raise FileNotFoundError(f"缺少 CPA 原稿：{path}")
    return Image.open(path).convert("RGBA")


def is_magenta(red, green, blue):
    return (red >= MAGENTA_RED_MIN and green <= MAGENTA_GREEN_MAX and blue >= MAGENTA_BLUE_MIN
            and red - green >= MAGENTA_CHANNEL_GAP and blue - green >= MAGENTA_CHANNEL_GAP)


def remove_magenta(image):
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue, alpha = pixels[x, y]
            keep = alpha and not is_magenta(red, green, blue)
            pixels[x, y] = (red, green, blue, OPAQUE if keep else 0)
    return image


def output_scroll(volume, versions):
    source = remove_magenta(raw("scroll", volume, versions))
    bounds = source.getbbox()
    if bounds is None:
        raise ValueError(f"{volume} 卷轴没有可见主体")
    item = source.crop(bounds)
    item.thumbnail((SCROLL_INSET, SCROLL_INSET), Image.Resampling.NEAREST)
    alpha = item.getchannel("A")
    # 透明像素 RGB 归零再量化，免得品红残色占掉调色板。
    color = Image.new("RGB", item.size, (0, 0, 0))
    color.paste(item.convert("RGB"), mask=alpha)
    item = color.quantize(colors=PALETTE_COLORS, dither=Image.Dither.NONE).convert("RGBA")
    item.putalpha(alpha)
    canvas = Image.new("RGBA", (SCROLL_SIZE, SCROLL_SIZE), (0, 0, 0, 0))
    canvas.alpha_composite(item, ((SCROLL_SIZE - item.width) // 2, (SCROLL_SIZE - item.height) // 2))
    canvas.putalpha(canvas.getchannel("A").point(lambda value: OPAQUE if value >= ALPHA_THRESHOLD else 0))
    save(canvas, TEXTURES / "item" / f"{volume}_scroll.png")


def output_background(volume, versions):
    source = raw("background", volume, versions)
    if source.size != BACKGROUND_SIZE:
        raise ValueError(f"{volume} 背景尺寸是 {source.size}，预期 {BACKGROUND_SIZE}")
    save(source.convert("RGB"), TEXTURES / "gui/scroll" / f"{volume}.png")


def output_technique(technique, versions):
    source = raw("icon", technique, versions).convert("RGB")
    if source.width != source.height:
        raise ValueError(f"{technique} 战斗图标必须是方图，实际 {source.size}")
    icon_64 = normalize_icon(source.resize((ICON_LARGE, ICON_LARGE), Image.Resampling.NEAREST))
    icon_32 = icon_64.resize((ICON_SIZE, ICON_SIZE), Image.Resampling.NEAREST)
    save(icon_32, TEXTURES / "gui/technique" / f"{technique}.png")
    save(icon_64, TEXTURES / "gui/technique" / f"{technique}_64.png")


def normalize_icon(image):
    # 把模型画出的暗底色差统一成本书底色，再重画两圈边框；中间主体原样保留。
    inner = image.crop((ICON_SAMPLE_MARGIN, ICON_SAMPLE_MARGIN,
                        ICON_LARGE - ICON_SAMPLE_MARGIN, ICON_LARGE - ICON_SAMPLE_MARGIN))
    dark = Counter(color for color in inner.get_flattened_data() if max(color) <= BACKGROUND_MAX_CHANNEL)
    if not dark:
        raise ValueError("战斗图标缺少暗色底")
    background = dark.most_common(1)[0][0]
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            distance = sum((channel - base) ** 2 for channel, base in zip(pixels[x, y], background))
            if distance <= BACKGROUND_COLOR_DISTANCE ** 2:
                pixels[x, y] = ICON_BACKGROUND
    limited = image.quantize(colors=PALETTE_COLORS, dither=Image.Dither.NONE).convert("RGB")
    frame = ImageDraw.Draw(limited)
    last = ICON_LARGE - 1
    frame.rectangle((0, 0, last, last), outline=OUTER_RIM, width=LARGE_RIM_WIDTH)
    frame.rectangle((LARGE_RIM_WIDTH, LARGE_RIM_WIDTH, last - LARGE_RIM_WIDTH, last - LARGE_RIM_WIDTH),
                    outline=INNER_RIM, width=LARGE_RIM_WIDTH)
    return limited


def save(image, target):
    # 重出时直接覆盖：成品全由原稿和本脚本决定，不手修。
    target.parent.mkdir(parents=True, exist_ok=True)
    image.save(target, format="PNG", optimize=True)
    print(target.relative_to(ROOT))


def parse_versions(arguments):
    # 不带等号的参数是默认版本，种类/名字=版本 单独指定某一张。
    versions = {None: "v1"}
    for argument in arguments:
        name, separator, version = argument.partition("=")
        if separator:
            versions[name] = version
        else:
            versions[None] = argument
    return versions


def main():
    versions = parse_versions(sys.argv[1:])
    for volume in VOLUMES:
        output_scroll(volume, versions)
        output_background(volume, versions)
    for technique in TECHNIQUES:
        output_technique(technique, versions)


if __name__ == "__main__":
    main()
