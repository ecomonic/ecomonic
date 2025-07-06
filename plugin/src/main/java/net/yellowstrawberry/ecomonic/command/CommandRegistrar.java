package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.yellowstrawberry.ecomonic.api.Ecomonic;
import net.yellowstrawberry.ecomonic.api.account.Account;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class CommandRegistrar {

    public CommandRegistrar(LifecycleEventManager<?> manager) {
        manager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(new MoneyCommand().build());
            commands.registrar().register(new AccountCommand().build());
        });
    }
}
