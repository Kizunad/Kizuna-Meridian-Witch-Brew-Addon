package dev.kizuna.meridian.witch.verification;

import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.ScrollDrop;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 发行目录经正式 Human 内容和 Technique 校验器完成注册。 */
public final class WitchCatalogTest {
    private static final int TECHNIQUES = 5;
    private static final int VOLUMES = 2;
    private static final double MOVEMENT = 0.4;
    private static final String DROPPED_BY = "minecraft:witch";
    private static final double SCROLL_DROP_CHANCE = 0.25;

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

    @Test
    public void everyVolumeDropsOnlyFromWitchesKilledByPlayers() throws IOException {
        // 生存来源：两卷都只向女巫声明一项，概率占位；死者会不会这一卷由 Threatening 学卷表决定。
        ScrollDrop drop = ScrollDrop.newBuilder()
                .setEntityType(DROPPED_BY)
                .setChance(SCROLL_DROP_CHANCE)
                .build();
        for (var volume : WitchCatalog.registry(WitchCatalog.load()).volumes()) {
            assertEquals(volume.getId(), List.of(drop), volume.getScrollDropsList());
        }
    }
}
