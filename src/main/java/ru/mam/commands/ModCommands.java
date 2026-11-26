package ru.mam.commands;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ru.mam.rules.PlayerRuleEngine;

public final class ModCommands {
    private ModCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("mamreset")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                            PlayerRuleEngine.reset(player);
                            context.getSource().sendFeedback(() -> Text.literal("Правила сброшены"), false);
                            return 1;
                        })));
    }
}
