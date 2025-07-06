package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class CommandUtils {
    public static RequiredArgumentBuilder<CommandSourceStack, ?> player() {
        return player("player");
    }

    @SuppressWarnings("unchecked")
    public static RequiredArgumentBuilder<CommandSourceStack, ?> player(String name) {
        return Commands.argument(name, StringArgumentType.word())
                .suggests((context, builder) -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        builder.suggest(player.getName());
                    }
                    return builder.buildFuture();
                });
    }
}
