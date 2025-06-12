package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.yellowstrawberry.ecomonic.api.Ecomonic;
import net.yellowstrawberry.ecomonic.api.account.Account;
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
                    Account a = Ecomonic.INSTANCE.getPrimaryAccount(p.getUniqueId());
                    if(a == null) {
                        p.sendMessage("You do not have an account!");
                        return 0;
                    }
                    double balance = a.getBalance();
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
            .then(
                Commands.argument("player", ArgumentTypes.player()).then(
                    Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(context -> {
                        if (context.getSource().getExecutor() instanceof Player p) {
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            if(Ecomonic.INSTANCE.deposit(Ecomonic.INSTANCE.getPrimaryAccount(p.getUniqueId()).getId(), amount)) {
                                p.sendMessage("You have given " + amount + " money to " + context.getArgument("player", Player.class).getName() + ".");
                            } else {
                                p.sendMessage("Failed to give money. Please check your balance.");
                            }
                            return 1;
                        } else {
                            context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + "You are not a player!"));
                            return 0;
                        }
                    })
                )
            )
        );

        root.then(
            Commands.literal("transfer").then(
                Commands.argument("from", LongArgumentType.longArg()).then(
                    Commands.argument("to", LongArgumentType.longArg()).then(
                        Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(context -> {
                            long fromId = LongArgumentType.getLong(context, "from");
                            long toId = LongArgumentType.getLong(context, "to");
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            boolean withdrawResult = Ecomonic.INSTANCE.withdraw(fromId, amount);
                            if (!withdrawResult) {
                                context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + " Failed to transfer. (Not enough balance)"));
                                return 0;
                            }
                            boolean depositResult = Ecomonic.INSTANCE.deposit(toId, amount);
                            if (!depositResult) {
                                Ecomonic.INSTANCE.deposit(fromId, amount);
                                context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + " Failed to transfer. (Account not found)"));
                                return 0;
                            }
                            context.getSource().getSender().sendMessage(Component.text(ChatColor.GREEN + "Successfully transferred " + amount + "."));
                            return 1;
                        })
                    )
                )
            )
        ).requires(sender -> sender.getSender().hasPermission("ecomonic.user.general"));


        root.then(Commands.literal("request"))
                .requires(sender -> sender.getSender().hasPermission("ecomonic.user.request"))
                .then(Commands.argument("player", ArgumentTypes.player()))
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)));
    }

    private void registerAdminCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(
            Commands.literal("set").requires(sender -> sender.getSender().hasPermission("ecomonic.admin")).then(
                Commands.argument("player", ArgumentTypes.player()).then(
                    Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(context -> {
                        if (context.getSource().getExecutor() instanceof Player p) {
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            Ecomonic.INSTANCE.getPrimaryAccount(p.getUniqueId()).set(amount);
                            p.sendMessage(p.getName()+"'s balance has been set to " + amount);
                            return 1;
                        } else {
                            context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + "You are not a player!"));
                            return 0;
                        }
                    })
                )
            )
        );

        root.then(
            Commands.literal("take").requires(sender -> sender.getSender().hasPermission("ecomonic.admin"))
                .then(
                    Commands.argument("player", ArgumentTypes.player()).then(
                        Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(context -> {
                            Player target = context.getArgument("player", Player.class);
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            if (target == null) {
                                context.getSource().getSender().sendMessage(Component.text(ChatColor.RED + " Target player not found!"));
                                return 0;
                            }
                            Ecomonic.INSTANCE.withdraw(Ecomonic.INSTANCE.getPrimaryAccount(target.getUniqueId()).getId(), amount);
                            context.getSource().getSender().sendMessage(Component.text(ChatColor.GREEN + "You have taken " + amount + " money from " + target.getName() + "."));
                            target.sendMessage(Component.text(ChatColor.RED + "An admin has taken " + amount + " money from your account."));
                            return 1;
                        })
                    )
                )
        );
    }
}
