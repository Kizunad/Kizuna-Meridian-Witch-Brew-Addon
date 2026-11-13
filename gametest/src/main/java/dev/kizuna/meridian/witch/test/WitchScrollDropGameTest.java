package dev.kizuna.meridian.witch.test;

import dev.kizuna.meridian.technique.fabric.TechniqueScrollDrops;
import dev.kizuna.meridian.technique.fabric.TechniqueScrollItem;
import dev.kizuna.meridian.technique.fabric.TechniqueScrollItems;
import dev.kizuna.meridian.technique.fabric.testing.RecordingConnection;
import dev.kizuna.meridian.technique.fabric.testing.TestPlayers;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 行药诀的生存来源：玩家亲手杀死会某一卷的女巫，才按目录概率掉这一卷。
 *
 * <p>女巫走 Threatening 的正式生成命令，按档位照学卷表学卷；掉落走原版女巫掉落表。</p>
 */
public final class WitchScrollDropGameTest {
    private static final String GUARD = "meridian_witch:guard";
    private static final String SEED = "meridian_witch:seed";
    private static final List<String> VOLUMES = List.of(GUARD, SEED);
    private static final List<Identifier> CLIENT_CHANNELS = List.of(Identifier.parse("meridian:body_view"),
            Identifier.parse("kizuna_players_animation:play"), Identifier.parse("kizuna_players_animation:stop"));
    private static final int TIMEOUT = 100;
    private static final int BIRTH_TICKS = 2;
    private static final int LOOT_ROLLS = 200;
    private static final int LEAST_DROPS = 25;
    private static final int MOST_DROPS = 75;
    private static final int LAST_HURT_TICKS = 100;

    @GameTest(maxTicks = TIMEOUT)
    public void witchesDropOnlyTheVolumesTheyKnow(GameTestHelper context) {
        // 真炁女巫会《养》《引》，游炁只会《养》，凡人一卷不会；玩家打死时会的卷各掉约四分之一，其余不掉。
        ServerLevel level = context.getLevel();
        ServerPlayer player = TestPlayers.detached(level, "witch_drop_test");
        RecordingConnection connection = new RecordingConnection(CLIENT_CHANNELS);
        TestPlayers.logIn(player, connection);
        Mob trueQi = spawn(context, "true_qi", new BlockPos(1, 2, 1));
        Mob wandering = spawn(context, "wandering_qi", new BlockPos(3, 2, 1));
        Mob mortal = spawn(context, "mortal", new BlockPos(5, 2, 1));
        context.runAfterDelay(BIRTH_TICKS, () -> {
            try {
                context.assertTrue(knows(trueQi, GUARD) && knows(trueQi, SEED), "真炁女巫没学会《养》《引》");
                context.assertTrue(knows(wandering, GUARD) && !knows(wandering, SEED), "游炁女巫学卷不符");
                context.assertTrue(VOLUMES.stream().noneMatch(volume -> knows(mortal, volume)), "凡人女巫也学了卷");
                DamageSource byPlayer = level.damageSources().playerAttack(player);
                for (Mob witch : List.of(trueQi, wandering, mortal)) {
                    Map<String, Integer> drops = roll(level, witch, byPlayer, player);
                    System.out.println("[行药诀掉落] 玩家击杀 " + LOOT_ROLLS + " 次：" + drops);
                    assertDropsFollowKnowledge(context, witch, drops);
                }
                DamageSource byMob = level.damageSources().mobAttack(mortal);
                context.assertTrue(total(roll(level, trueQi, byMob, null)) == 0, "不是玩家打死的女巫也掉卷");
                context.succeed();
            } finally {
                connection.disconnectFromClient();
            }
        });
    }

    private static Mob spawn(GameTestHelper context, String tier, BlockPos relative) {
        // 走 Threatening 的正式生成命令，按档位生出有身体、照学卷表学过卷的女巫；关掉 AI 免得它喝药扔药。
        ServerLevel level = context.getLevel();
        Vec3 position = context.absoluteVec(Vec3.atBottomCenterOf(relative));
        MinecraftServer server = level.getServer();
        CommandSourceStack source = server.createCommandSourceStack().withLevel(level).withSuppressedOutput();
        String command = String.format(Locale.ROOT, "meridian humanoid spawn minecraft:witch %s %.2f %.2f %.2f",
                tier, position.x, position.y, position.z);
        server.getCommands().performPrefixedCommand(source, command);
        List<Mob> spawned = level.getEntitiesOfClass(Mob.class, new AABB(BlockPos.containing(position)),
                mob -> mob.getType() == EntityTypes.WITCH);
        if (spawned.size() != 1) {
            throw new IllegalStateException("生成命令没有在原地生出一只女巫：" + command + " → " + spawned);
        }
        Mob witch = spawned.getFirst();
        witch.setNoAi(true);
        return witch;
    }

    private static void assertDropsFollowKnowledge(GameTestHelper context, Mob witch, Map<String, Integer> drops) {
        // 会的卷掉落次数落在 25% 的正负四个标准差里，不会的卷一次不掉。
        for (String volume : VOLUMES) {
            int count = drops.get(volume);
            if (knows(witch, volume)) {
                context.assertTrue(count >= LEAST_DROPS && count <= MOST_DROPS, volume + " 掉落偏离 25%：" + count);
            } else {
                context.assertTrue(count == 0, "不会 " + volume + " 的女巫也掉了这一卷：" + count);
            }
        }
    }

    private static boolean knows(Mob witch, String volume) {
        // 和掉落条件同一判定：功法服务里学会了本卷全部式。
        return new TechniqueScrollDrops.KnowsVolume(volume).knows(witch);
    }

    private static Map<String, Integer> roll(ServerLevel level, Mob mob, DamageSource source, ServerPlayer killer) {
        // 用原版掉落入口抽 LOOT_ROLLS 次，按卷数掉出来的卷轴；killer 为空算非玩家击杀。
        boolean playerKilled = killer != null;
        if (playerKilled) {
            mob.setLastHurtByPlayer(killer, LAST_HURT_TICKS);
        }
        ResourceKey<LootTable> table = EntityTypes.WITCH.getDefaultLootTable().orElseThrow();
        List<ItemStack> drops = new ArrayList<>();
        for (int roll = 0; roll < LOOT_ROLLS; roll++) {
            mob.dropFromLootTable(level, source, playerKilled, table, drops::add);
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String volume : VOLUMES) {
            TechniqueScrollItem scroll = scrollItem(volume);
            counts.put(volume, drops.stream().filter(stack -> stack.is(scroll)).mapToInt(ItemStack::getCount).sum());
        }
        return counts;
    }

    private static TechniqueScrollItem scrollItem(String volume) {
        // 功法库按卷生成的卷轴物品。
        TechniqueScrollItem item = TechniqueScrollItems.scrolls().get(Identifier.parse(volume + "_scroll"));
        if (item == null) {
            throw new IllegalStateException("没有卷轴物品：" + volume);
        }
        return item;
    }

    private static int total(Map<String, Integer> counts) {
        // 两卷掉落合计。
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
