package dev.kizuna.meridian.witch.verification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 五式正文：卷轴上每式一段老药婆讲的话，各不相同、不是旧占位；
 * 世界内文本不许出现分数、倍率、按键、界面这些机制说法，也不许夹英文。
 */
public final class WitchLoreTextTest {
    private static final int TECHNIQUES = 5;
    private static final String PLACEHOLDER = "以炁行药，药力循身而行。";
    private static final List<String> MECHANIC_WORDS = List.of(
            "分数", "倍率", "机制", "判定", "时窗", "冷却", "动作栏", "窗口", "界面", "按键",
            "右键", "左键", "主手", "副手", "玩家", "生物", "实体", "×", "%");
    private static final Pattern LATIN = Pattern.compile("[A-Za-z]");

    @Test
    public void everyTechniqueHasItsOwnText() throws IOException {
        // 五式各有一段，彼此不同，也不再是五式共用的那句占位。
        var definitions = WitchCatalog.registry(WitchCatalog.load()).definitions();
        assertEquals(TECHNIQUES, definitions.size());
        Set<String> texts = new HashSet<>();
        for (var definition : definitions) {
            String text = definition.getPresentation().getDescription();
            assertFalse(definition.getId() + " 正文是空的", text.isBlank());
            assertFalse(definition.getId() + " 还是占位", text.equals(PLACEHOLDER));
            texts.add(text);
        }
        assertEquals("有两式正文相同", TECHNIQUES, texts.size());
    }

    @Test
    public void textUsesNoMechanicWords() throws IOException {
        // 找出每式正文里的机制说法和英文字母，一处都不许有。
        List<String> found = new ArrayList<>();
        for (var definition : WitchCatalog.registry(WitchCatalog.load()).definitions()) {
            String text = definition.getPresentation().getDescription();
            for (String word : MECHANIC_WORDS) {
                if (text.contains(word)) {
                    found.add(definition.getId() + "：" + word);
                }
            }
            if (LATIN.matcher(text).find()) {
                found.add(definition.getId() + "：英文字母");
            }
        }
        assertTrue("正文里有机制说法：" + found, found.isEmpty());
    }
}
