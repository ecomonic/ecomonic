package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.yellowstrawberry.ecomonic.EcomonicPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class CommandRegistrar {

    public CommandRegistrar(LifecycleEventManager<?> manager) {
        manager.registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(buildCommands());
        });
    }

    public LiteralCommandNode<CommandSourceStack> buildCommands() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("money")
                .requires(sender -> sender.getSender().hasPermission("ecomonic.user"))
                .executes(context -> {
                    if (context.getSource().getExecutor() instanceof Player p) {
                        double balance = EcomonicPlugin.ecomonic.getPrimaryAccount(p.getUniqueId()).getBalance();
                        p.sendMessage("Your balance is: " + balance);
                        return 1;
                    }else {
                        context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + "You are not a player!"));
                        return 0;
                    }
                });

        registerUserCommands(root);
        registerAdminCommands(root);

        return root.build();
    }

    private void registerUserCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("give")
                .requires(sender -> sender.getExecutor() != null && sender.getExecutor().hasPermission("ecomonic.user.general"))
                .then(Commands.argument("player", ArgumentTypes.player()))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)))
                .executes(context -> {
                    if (context.getSource().getExecutor() instanceof Player p) {
                        double amount = DoubleArgumentType.getDouble(context, "amount");
                        if(EcomonicPlugin.ecomonic.deposit(EcomonicPlugin.ecomonic.getPrimaryAccount(p.getUniqueId()).getId(), amount)) {
                            p.sendMessage("You have given " + amount + " money to " + context.getArgument("player", Player.class).getName() + ".");
                        } else {
                            p.sendMessage("Failed to give money. Please check your balance.");
                        }
                        return 1;
                    } else {
                        context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + "You are not a player!"));
                        return 0;
                    }
                }));

        root.then(Commands.literal("transfer"))
                .requires(sender -> sender.getSender().hasPermission("ecomonic.user.general"))
                .then(Commands.argument("from", LongArgumentType.longArg())
                        .then(Commands.argument("to", LongArgumentType.longArg())
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)))
                                .executes(context -> {
                                    //TODO: Implement transfer logic
                                    return 0;
                                })));

        root.then(Commands.literal("request"))
                .requires(sender -> sender.getSender().hasPermission("ecomonic.user.request"))
                .then(Commands.argument("player", ArgumentTypes.player()))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)));
    }

    private void registerAdminCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("reload"))
                .requires(sender -> sender.getSender().hasPermission("ecomonic.admin"))
                .executes(context -> {
                    // TODO: Implement reload logic
                    return 0;
                });

        root.then(Commands.literal("set"))
                .requires(sender -> sender.getSender().hasPermission("ecomonic.admin"))
                .then(Commands.argument("player", ArgumentTypes.player()))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)))
                .executes(context -> {
                    if (context.getSource().getExecutor() instanceof Player p) {
                        double amount = DoubleArgumentType.getDouble(context, "amount");
                        EcomonicPlugin.ecomonic.set(EcomonicPlugin.ecomonic.getPrimaryAccount(p.getUniqueId()).getId(), amount);
                        p.sendMessage("Your balance has been set to " + amount);
                        return 1;
                    } else {
                        context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + "You are not a player!"));
                        return 0;
                    }
                });

        root.then(Commands.literal("take"))
                .requires(sender -> sender.getSender().hasPermission("ecomonic.admin"))
                .then(Commands.argument("player", ArgumentTypes.player()))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)))
                .executes(context -> {
                    if (context.getSource().getExecutor() instanceof Player p) {
                        double amount = DoubleArgumentType.getDouble(context, "amount");
                        EcomonicPlugin.ecomonic.withdraw(EcomonicPlugin.ecomonic.getPrimaryAccount(p.getUniqueId()).getId(), amount);
                        p.sendMessage("You have taken " + amount + " money.");
                        return 1;
                    } else {
                        context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + "You are not a player!"));
                        return 0;
                    }
                });
    }
}
