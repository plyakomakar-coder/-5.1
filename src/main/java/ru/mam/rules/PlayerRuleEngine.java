package ru.mam.rules;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import ru.mam.network.RulesPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Хранит активные правила каждого игрока. */
public final class PlayerRuleEngine {
    private static final Map<UUID, List<Rule>> ACTIVE = new HashMap<>();
    private static final int MAX_RULES = 3;

    private PlayerRuleEngine() {
    }

    /** Вызывается при возрождении: случайный набор из 1–3 правил с учётом весов. */
    public static void rollNew(ServerPlayerEntity player) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<RuleRegistry.Template> pool = new ArrayList<>(RuleRegistry.ALL);
        int count = 1 + random.nextInt(MAX_RULES);

        List<Rule> chosen = new ArrayList<>();
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            int total = 0;
            for (RuleRegistry.Template t : pool) {
                total += t.weight();
            }
            int roll = random.nextInt(total);
            int index = 0;
            for (int j = 0; j < pool.size(); j++) {
                roll -= pool.get(j).weight();
                if (roll < 0) {
                    index = j;
                    break;
                }
            }
            RuleRegistry.Template picked = pool.remove(index);
            chosen.add(picked.factory().get());
        }

        ACTIVE.put(player.getUuid(), chosen);
        for (Rule rule : chosen) {
            rule.apply(player);
        }

        MutableText message = Text.literal("Новые правила: ");
        for (int i = 0; i < chosen.size(); i++) {
            Rule rule = chosen.get(i);
            if (i > 0) {
                message.append(Text.literal(", "));
            }
            message.append(Text.literal(rule.description())
                    .formatted(rule.harmful() ? Formatting.RED : Formatting.GREEN));
        }
        player.sendMessage(message, false);

        sendOverlay(player, chosen);

        player.getEntityWorld().playSound((PlayerEntity) null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    private static void sendOverlay(ServerPlayerEntity player, List<Rule> rules) {
        if (!ServerPlayNetworking.canSend(player, RulesPayload.ID)) {
            return;
        }
        StringBuilder text = new StringBuilder();
        for (Rule rule : rules) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(rule.harmful() ? '1' : '0').append(rule.description());
        }
        ServerPlayNetworking.send(player, new RulesPayload(text.toString()));
    }

    /** Вызывается при смерти: правила забываются (эффекты уходят вместе с игроком). */
    public static void clear(UUID playerId) {
        ACTIVE.remove(playerId);
    }

    /** Вызывается командой /mamreset: снимает действие всех правил. */
    public static void reset(ServerPlayerEntity player) {
        List<Rule> rules = ACTIVE.remove(player.getUuid());
        if (rules != null) {
            for (Rule rule : rules) {
                rule.remove(player);
            }
        }
    }

    /** Вызывается каждый тик сервера. */
    public static void tickAll(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            for (Rule rule : ACTIVE.getOrDefault(player.getUuid(), List.of())) {
                rule.tick(player);
            }
        }
        ChestTraps.tick(server);
    }

    /** Вызывается при ломании блока. */
    public static void onBlockBroken(ServerPlayerEntity player, BlockPos pos) {
        for (Rule rule : ACTIVE.getOrDefault(player.getUuid(), List.of())) {
            rule.onBlockBroken(player, pos);
        }
    }
}
