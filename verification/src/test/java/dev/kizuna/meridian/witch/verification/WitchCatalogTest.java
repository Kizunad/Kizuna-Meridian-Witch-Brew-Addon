package dev.kizuna.meridian.witch.verification;

import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.PresentationDuration;
import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.PresentationEffect;
import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.ScrollDrop;
import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.TechniqueDefinition;
import dev.kizuna.meridian.technique.protocol.pb.TechniqueMessages.TechniqueEffect;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
    private static final String ANOINT = "meridian_witch:anoint";
    private static final String ANOINT_START = "meridian_brewing:anoint_start";
    /** 四式各接 PA 一段剪辑；敷药分两臂、两腿两段，由炼药起手选组后播。 */
    private static final Map<String, String> CLIPS = Map.of(
            "meridian_witch:imbibe", "meridian_witch:witch_imbibe",
            "meridian_witch:lob_seed", "meridian_witch:witch_lob_seed",
            "meridian_witch:fling_seed", "meridian_witch:witch_fling_seed",
            "meridian_witch:spray_trigger", "meridian_witch:witch_spray_trigger");

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

    @Test
    public void everyTechniqueStartsItsClipAtCastStartAndHoldsItThroughRecovery() throws IOException {
        // 唤魔者 C6c 同一写法：起手 0 毫秒开播，收势结束停；敷药没有表现效果，改由起手那一条用药交给炼药。
        var registry = WitchCatalog.registry(WitchCatalog.load());
        for (var entry : CLIPS.entrySet()) {
            var definition = registry.findLatest(entry.getKey()).orElseThrow();
            PresentationEffect expected = PresentationEffect.newBuilder()
                    .setId(entry.getValue())
                    .setDuration(PresentationDuration.UNTIL_RECOVERY_END)
                    .setStartMillis(0)
                    .build();
            assertEquals(entry.getKey(), List.of(expected), presentations(definition));
            assertEquals(entry.getKey(), 0, presentationEffects(definition).getFirst().getAtMillis());
        }
        var anoint = registry.findLatest(ANOINT).orElseThrow();
        assertTrue(presentations(anoint).isEmpty());
        var starts = anoint.getEffectsList().stream()
                .filter(effect -> effect.getUseBrew().getProcessorId().equals(ANOINT_START)).toList();
        assertEquals(1, starts.size());
        assertEquals(0, starts.getFirst().getAtMillis());
        assertEquals(anoint.getBrewItem().getKindIdsList(), starts.getFirst().getUseBrew().getBrewKindIdsList());
    }

    private static List<TechniqueEffect> presentationEffects(TechniqueDefinition definition) {
        // 只取表现效果，其余声音、用药、做功按各自用例核对。
        return definition.getEffectsList().stream().filter(TechniqueEffect::hasPresentation).toList();
    }

    private static List<PresentationEffect> presentations(TechniqueDefinition definition) {
        // 比较完整消息，剪辑名、持续方式和起播毫秒任一项不对都会报出来。
        return presentationEffects(definition).stream().map(TechniqueEffect::getPresentation).toList();
    }
}
