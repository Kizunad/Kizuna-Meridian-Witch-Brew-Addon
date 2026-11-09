package dev.kizuna.meridian.witch.verification;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 发行目录经正式 Human 内容和 Technique 校验器完成注册。 */
public final class WitchCatalogTest {
    private static final int TECHNIQUES = 5;
    private static final int VOLUMES = 2;
    private static final double MOVEMENT = 0.4;

    @Test
    public void productionCatalogRegistersTwoVolumesAndFiveTechniques() throws IOException {
        // 校验书卷前置、正文目录和全程限速，不另造一份测试功法。
        var registry = WitchCatalog.registry(WitchCatalog.load());
        assertEquals(1, registry.books().size());
        assertEquals(VOLUMES, registry.volumes().size());
        assertEquals(TECHNIQUES, registry.definitions().size());
        var middle = registry.findVolume("meridian_witch:seed").orElseThrow();
        assertEquals("meridian_witch:guard", middle.getPrerequisiteVolumeIds(0));
        for (var definition : registry.definitions()) {
            assertEquals(MOVEMENT, definition.getMovementSpeedMultiplier(), 0);
            assertTrue(definition.hasBrewItem());
        }
    }
}
