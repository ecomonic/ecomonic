package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.yellowstrawberry.ecomonic.api.Ecomonic;
import net.yellowstrawberry.ecomonic.api.account.Account;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@SuppressWarnings("UnstableApiUsage")
public class MoneyCommand implements CommandRoot{
    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("money")
                .requires(sender -> sender.getSender().hasPermission("ecomonic.money"))
                .executes(context -> {
                    if (context.getSource().getExecutor() instanceof Player p) {
                        Account a = Ecomonic.INSTANCE.getPrimaryAccount(p.getUniqueId());
                        if(a == null) {
                            p.sendMessage(Component.text("You do not have an account!", TextColor.color(0xFF0000)));
                            return 0;
                        }
                        double balance = a.getBalance();
                        p.sendMessage("Your balance is " + balance);
                        return 1;
                    }else {
                        context.getSource().getSender().sendMessage(Component.text("You are not a player!", TextColor.color(0xFF0000)));
                        return 0;
                    }
                });

        registerUserCommands(root);
        registerAdminCommands(root.then(Commands.literal("admin")).requires(sender -> sender.getSender().hasPermission("ecomonic.admin")));

        return root.build();
    }

    private void registerUserCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(
            Commands.literal("send")
                    .requires(sender -> sender.getSender().hasPermission("ecomonic.money.send"))
                    .then(
                        CommandUtils.player("to").then(
                            Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(context -> {
                                if(context.getSource().getExecutor() instanceof Player p) {
                                    Player to = Bukkit.getPlayer(StringArgumentType.getString(context, "to"));
                                    if(to == null) {
                                        context.getSource().getSender().sendMessage(Component.text("Player not found!", TextColor.color(0xFF0000)));
                                        return 0;
                                    }

                                    Account fromAccount = Ecomonic.INSTANCE.getPrimaryAccount(p.getUniqueId());
                                    Account toAccount = Ecomonic.INSTANCE.getPrimaryAccount(to.getUniqueId());
                                    double amount = DoubleArgumentType.getDouble(context, "amount");
                                    boolean withdrawResult = fromAccount.withdraw(amount);
                                    if (!withdrawResult) {
                                        context.getSource().getSender().sendMessage(Component.text("Failed to transfer. (Not enough balance)", TextColor.color(0xFF0000)));
                                        return 0;
                                    }
                                    boolean depositResult = toAccount.deposit(amount);
                                    if (!depositResult) {
                                        fromAccount.deposit(amount);
                                        context.getSource().getSender().sendMessage(Component.text("Failed to transfer. (Account not found)", TextColor.color(0xFF0000)));
                                        return 0;
                                    }
                                    context.getSource().getSender().sendMessage(Component.text("Successfully transferred " + amount + ".", TextColor.color(0x00FF00)));
                                    if(to.isOnline()) to.sendMessage(Component.text("You have received " + amount + " from " + p.getName() + ".", TextColor.color(0x00FF00)));
                                    return 1;
                                }else {
                                    context.getSource().getSender().sendMessage(Component.text("You're not a player", TextColor.color(0xFF0000)));
                                    return 0;
                                }
                            })
                        )
            )
        );


        root.then(
            Commands.literal("request")
                .requires(sender -> sender.getSender().hasPermission("ecomonic.money.request"))
                .then(
                    CommandUtils.player()
                        .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0)))
                        .executes(context -> {
                            if(context.getSource().getExecutor() instanceof Player p) {
                                Player to = Bukkit.getPlayer(StringArgumentType.getString(context, "to"));
                                double amount = DoubleArgumentType.getDouble(context, "amount");
                                if(to == null) {
                                    context.getSource().getSender().sendMessage(Component.text("Player not found!", TextColor.color(0xFF0000)));
                                    return 0;
                                }

                                to.sendMessage(Component.textOfChildren(
                                        p.displayName(), Component.text(" requested %.2f. ".formatted(amount)), Component.text("[Click Here]", TextColor.color(0xFFFF00)).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/money send %s %.2f".formatted(p.getName(), amount))), Component.text(" to send requested amount of money.")
                                ));
                                p.sendMessage(Component.text("Sent request to " + to.getName() + ".").color(TextColor.color(0x00FF00)));
                                return 1;
                            }else {
                                context.getSource().getSender().sendMessage(Component.text("You're not a player", TextColor.color(0xFF0000)));
                                return 0;
                            }
                        })
                )
        );
    }

    private void registerAdminCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(
            Commands.literal("set")
                .requires(sender -> sender.getSender().hasPermission("ecomonic.admin.set"))
                .then(
                    CommandUtils.player().then(
                        Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(context -> {
                            Player p = Bukkit.getPlayer(StringArgumentType.getString(context, "player"));
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            Ecomonic.INSTANCE.getPrimaryAccount(p.getUniqueId()).set(amount);
                            p.sendMessage(Component.text(p.getName()+"'s balance has been set to " + amount+".", TextColor.color(0x00FF00)));
                            p.sendMessage(Component.text("An admin has updated your balance to " + amount + ".", TextColor.color(0xFF0000)));
                            return 1;
                        })
                    )
                )
        );

        root.then(
            Commands.literal("take")
                .requires(sender -> sender.getSender().hasPermission("ecomonic.admin.take"))
                .then(
                    CommandUtils.player().then(
                        Commands.argument("amount", DoubleArgumentType.doubleArg(0)).executes(context -> {
                            Player p = Bukkit.getPlayer(StringArgumentType.getString(context, "player"));
                            double amount = DoubleArgumentType.getDouble(context, "amount");
                            if (p == null) {
                                context.getSource().getSender().sendMessage(Component.text("Target player not found!", TextColor.color(0xFF0000)));
                                return 0;
                            }
                            Ecomonic.INSTANCE.withdraw(Ecomonic.INSTANCE.getPrimaryAccount(p.getUniqueId()).getId(), amount);
                            context.getSource().getSender().sendMessage(Component.text("You have taken " + amount + " money from " + p.getName() + ".", TextColor.color(0x00FF00)));
                            p.sendMessage(Component.text("An admin has taken " + amount + " money from your account.", TextColor.color(0xFF0000)));
                            return 1;
                        })
                    )
                )
        );
    }
}
