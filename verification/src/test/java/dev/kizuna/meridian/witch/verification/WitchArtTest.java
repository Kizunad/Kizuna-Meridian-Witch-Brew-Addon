package dev.kizuna.meridian.witch.verification;

import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.TechniqueCatalog;
import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.TechniqueDefinition;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** 每卷的卷轴物品图、阅读背景和每式的战斗图标都在正式包里，尺寸与功法库约定一致。 */
public final class WitchArtTest {
    private static final String NAMESPACE = "meridian_witch";
    private static final String PREFIX = NAMESPACE + ":";
    private static final String TEXTURES = "assets/" + NAMESPACE + "/textures/";
    private static final String FABRIC_RESOURCES = "witch.fabricResources";
    private static final int SCROLL_PIXELS = 32;
    private static final int BACKGROUND_WIDTH = 1536;
    private static final int BACKGROUND_HEIGHT = 1024;
    private static final int TECHNIQUE_ICON_PIXELS = 32;
    private static final int LARGE_TECHNIQUE_ICON_PIXELS = 64;

    @Test
    public void scrollsBackgroundsAndTechniqueIconsCoverTheCatalog() throws IOException {
        // 卷轴物品 ID 是 <卷>_scroll，背景按卷路径取；每式 icon_id 与式 ID 同名，带 32 和 64 两档。
        TechniqueCatalog catalog = WitchCatalog.load();
        for (var volume : catalog.getVolumesList()) {
            String path = volume.getId().substring(PREFIX.length());
            assertImage(TEXTURES + "item/" + path + "_scroll.png", SCROLL_PIXELS, SCROLL_PIXELS);
            assertImage(TEXTURES + "gui/scroll/" + path + ".png", BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        }
        for (TechniqueDefinition definition : catalog.getDefinitionsList()) {
            String icon = definition.getPresentation().getIconId();
            assertEquals(definition.getId(), definition.getId(), icon);
            String path = TEXTURES + "gui/technique/" + icon.substring(PREFIX.length());
            assertImage(path + ".png", TECHNIQUE_ICON_PIXELS, TECHNIQUE_ICON_PIXELS);
            assertImage(path + "_64.png", LARGE_TECHNIQUE_ICON_PIXELS, LARGE_TECHNIQUE_ICON_PIXELS);
        }
    }

    private static void assertImage(String path, int width, int height) throws IOException {
        // 贴图能被 ImageIO 解码，尺寸与约定一致。
        Path file = fabricResources().resolve(path);
        assertTrue(file.toString(), Files.isRegularFile(file));
        BufferedImage image = ImageIO.read(file.toFile());
        assertNotNull(file.toString(), image);
        assertEquals(path, width, image.getWidth());
        assertEquals(path, height, image.getHeight());
    }

    private static Path fabricResources() {
        // 构建脚本传入 fabric 模块资源根目录；缺少时说明不是经 Gradle 运行。
        String directory = System.getProperty(FABRIC_RESOURCES);
        if (directory == null) {
            throw new IllegalStateException("缺少系统属性 " + FABRIC_RESOURCES);
        }
        return Path.of(directory);
    }
}
