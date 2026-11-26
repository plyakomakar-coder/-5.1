package ru.mam;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;
import ru.mam.commands.ModCommands;
import ru.mam.network.RulesPayload;
import ru.mam.rules.ChestTraps;
import ru.mam.rules.PeacefulMobs;
import ru.mam.rules.PlayerRuleEngine;

public class DeathRulesMod implements ModInitializer {
    public static final String MOD_ID = "mam";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(RulesPayload.ID, RulesPayload.CODEC);

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                PlayerRuleEngine.rollNew(newPlayer));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                PlayerRuleEngine.clear(player.getUuid());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(PlayerRuleEngine::tickAll);

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                PlayerRuleEngine.onBlockBroken(serverPlayer, pos);
            }
        });

        UseBlockCallback.EVENT.register(ChestTraps::onUse);

        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) ->
                PeacefulMobs.onDamage(entity, source));

        ModCommands.register();
    }
}
