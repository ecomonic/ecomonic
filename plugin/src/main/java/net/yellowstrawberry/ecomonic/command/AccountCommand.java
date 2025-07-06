package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.yellowstrawberry.ecomonic.api.Ecomonic;
import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.config.Configurations;
import org.bukkit.entity.Player;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class AccountCommand implements CommandRoot {
    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("account")
                .then(buildListCommand())
                .then(buildCreateCommand())
                .then(buildDeleteCommand())
                .then(buildTransferCommand())
                .then(buildAdminCommands())
                .build();
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildListCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("list")
                .requires(source -> source.getSender().hasPermission("ecomonic.account.list"))
                .executes(this::executeList);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildTransferCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("transfer")
                .requires(source -> source.getSender().hasPermission("ecomonic.account.transfer"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("from", LongArgumentType.longArg())
                        .suggests((context, builder) -> {
                            Ecomonic.INSTANCE.getAccounts(context.getSource().getExecutor().getUniqueId()).stream().map(Account::getId).map(String::valueOf).forEachOrdered(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("to", LongArgumentType.longArg())
                                .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("amount", DoubleArgumentType.doubleArg(0.0))
                                        .executes(this::executeTransfer))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildCreateCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("create")
                .requires(source -> source.getSender().hasPermission("ecomonic.account.create"))
                .executes(this::executeCreateSelf);
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildPrimaryCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("primary")
                .requires(source -> source.getSender().hasPermission("ecomonic.account.primary"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("accountId", LongArgumentType.longArg())
                .suggests((context, builder) -> {
                    Ecomonic.INSTANCE.getAccounts(context.getSource().getExecutor().getUniqueId()).stream().map(Account::getId).map(String::valueOf).forEachOrdered(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(this::executePrimary));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildDeleteCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("delete")
                .requires(source -> source.getSender().hasPermission("ecomonic.account.delete"))
                .then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("accountId", LongArgumentType.longArg())
                .suggests((context, builder) -> {
                    Ecomonic.INSTANCE.getAccounts(context.getSource().getExecutor().getUniqueId()).stream().map(Account::getId).map(String::valueOf).forEachOrdered(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(this::executeDeleteSelf));
    }


    private LiteralArgumentBuilder<CommandSourceStack> buildAdminCommands() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("admin")
                .requires(source -> source.getSender().hasPermission("ecomonic.admin"))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("create").requires(source -> source.getSender().hasPermission("ecomonic.admin.create"))
                        .then(CommandUtils.player()
                                .executes(this::executeCreate)))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("delete").requires(source -> source.getSender().hasPermission("ecomonic.admin.delete"))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("accountId", LongArgumentType.longArg())
                                .executes(this::executeDelete)))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("set").requires(source -> source.getSender().hasPermission("ecomonic.admin.set"))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, Long>argument("accountId", LongArgumentType.longArg())
                                .then(RequiredArgumentBuilder.<CommandSourceStack, Double>argument("amount", DoubleArgumentType.doubleArg())
                                        .executes(this::executeSet))));
    }

    private int executeCreateSelf(CommandContext<CommandSourceStack> context) {
        try {
            Player player = context.getSource().getExecutor() instanceof Player ?
                    (Player) context.getSource().getExecutor() : null;
            if (player == null) {
                context.getSource().getExecutor().sendMessage(Component.text("This command can only be used by players", TextColor.color(0xFF5555)));
                return 0;
            }

            List<Account> existing = Ecomonic.INSTANCE.getAccounts(player.getUniqueId());
            if (existing.size() >= Configurations.maxAccounts) {
                context.getSource().getExecutor().sendMessage(Component.text("You have reached the maximum number of accounts (" + Configurations.maxAccounts + ")", TextColor.color(0xFF5555)));
                return 0;
            }

            Account newAccount = Ecomonic.INSTANCE.createAccount(player.getUniqueId());
            if (existing.isEmpty()) {
                Ecomonic.INSTANCE.setPrimaryAccount(player.getUniqueId(), newAccount);
            }

            context.getSource().getExecutor().sendMessage(Component.text("Created new account with ID: " + newAccount.getId(), TextColor.color(0x55FF55)));
            return 1;
        } catch (Exception e) {
            context.getSource().getExecutor().sendMessage(Component.text("Error: " + e.getMessage()).color(TextColor.color(0xFF5555)));
            return 0;
        }
    }

    private int executePrimary(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getExecutor() instanceof Player ? (Player) context.getSource().getExecutor() : null;
        if (player == null) {
            context.getSource().getExecutor().sendMessage(Component.text("This command can only be used by players",TextColor.color(0xFF5555)));
            return 0;
        }

        long accountId = LongArgumentType.getLong(context, "accountId");
        Account account = Ecomonic.INSTANCE.getAccount(accountId);

        if (account == null) {
            context.getSource().getExecutor().sendMessage(Component.text("Account not found", TextColor.color(0xFF5555)));
            return 0;
        }

        Ecomonic.INSTANCE.setPrimaryAccount(player.getUniqueId(), account);
        context.getSource().getExecutor().sendMessage(Component.text("Set account " + accountId + " as primary", TextColor.color(0x55FF55)));

        return 1;
    }

    private int executeDeleteSelf(CommandContext<CommandSourceStack> context) {
        try {
            Player player = context.getSource().getExecutor() instanceof Player ? (Player) context.getSource().getExecutor() : null;
            if (player == null) {
                context.getSource().getExecutor().sendMessage(Component.text("This command can only be used by players",TextColor.color(0xFF5555)));
                return 0;
            }


            long accountId = LongArgumentType.getLong(context, "accountId");
            Account account = Ecomonic.INSTANCE.getAccount(accountId);

            if (account == null) {
                context.getSource().getExecutor().sendMessage(Component.text("Account not found", TextColor.color(0xFF5555)));
                return 0;
            }

            if (!player.hasPermission("ecomonic.admin") && !player.getUniqueId().equals(account.getOwner())) {
                context.getSource().getExecutor().sendMessage(Component.text("You don't own this account", TextColor.color(0xFF5555)));
                return 0;
            }

            if (Ecomonic.INSTANCE.getAccounts(player.getUniqueId()).size() <= 1 &&
                    Ecomonic.INSTANCE.getPrimaryAccount(player.getUniqueId()).getId() == accountId) {
                context.getSource().getExecutor().sendMessage(Component.text("Cannot delete your only account", TextColor.color(0xFF5555)));
                return 0;
            }

            if (account.getBalance() > 0) {
                context.getSource().getExecutor().sendMessage(Component.text("Cannot delete account with non-zero balance", TextColor.color(0xFF5555)));
                return 0;
            }

            if (Ecomonic.INSTANCE.deleteAccount(accountId) != null) {
                context.getSource().getExecutor().sendMessage(Component.text("Account " + accountId + " has been deleted").color(TextColor.color(0x55FF55)));
                return 1;
            } else {
                context.getSource().getExecutor().sendMessage(Component.text("Failed to delete account", TextColor.color(0xFF5555)));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().getExecutor().sendMessage(Component.text("Error: " + e.getMessage()).color(TextColor.color(0xFF5555)));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        try {
            Player player = context.getSource().getExecutor() instanceof Player ?
                    (Player) context.getSource().getExecutor() : null;
            if (player == null) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("This command can only be used by players")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            List<Account> accounts = Ecomonic.INSTANCE.getAccounts(player.getUniqueId());
            
            if (accounts.isEmpty()) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("You don't have any accounts")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            context.getSource().getExecutor().sendMessage(Component.text("Your accounts:"));
            Account primaryAccount = Ecomonic.INSTANCE.getPrimaryAccount(player.getUniqueId());
            for (Account account : accounts) {
                boolean isPrimary = primaryAccount != null && primaryAccount.getId() == account.getId();
                context.getSource().getExecutor().sendMessage(Component.text(String.format("ID: %d, Balance: %.2f%s", account.getId(), account.getBalance(),isPrimary ? " (Primary)" : ""), TextColor.color(0x55FF55)));
            }
            return 1;
        } catch (Exception e) {
            context.getSource().getExecutor().sendMessage(Component.text("Error: " + e.getMessage(), TextColor.color(0xFF5555)));
            return 0;
        }
    }

    private int executeTransfer(CommandContext<CommandSourceStack> context) {
        try {
            Player player = context.getSource().getExecutor() instanceof Player ?
                    (Player) context.getSource().getExecutor() : null;
            if (player == null) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("This command can only be used by players")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            long fromId = LongArgumentType.getLong(context, "from");
            long toId = LongArgumentType.getLong(context, "to");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            if (amount < Configurations.minTransactionAmount) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("Amount is below minimum transaction amount")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            if (amount > Configurations.maxTransactionAmount) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("Amount exceeds maximum transaction amount")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            Account fromAccount = Ecomonic.INSTANCE.getAccount(fromId);
            Account toAccount = Ecomonic.INSTANCE.getAccount(toId);

            if (fromAccount == null || toAccount == null) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("One or both accounts not found")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            if (!player.getUniqueId().equals(fromAccount.getOwner())) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("You don't own the source account")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            double totalAmount = amount;
            if (Configurations.enableTransactionFees) {
                totalAmount += amount * (Configurations.transactionFeePercentage / 100.0);
            }

            if (!fromAccount.has(totalAmount)) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("Insufficient funds")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            if (fromAccount.withdraw(totalAmount) && toAccount.deposit(amount)) {
                context.getSource().getExecutor().sendMessage(
                    Component.text(String.format("Successfully transferred %.2f from account %d to %d",
                        amount, fromId, toId))
                        .color(TextColor.color(0x55FF55))
                );
                return 1;
            } else {
                context.getSource().getExecutor().sendMessage(
                    Component.text("Transfer failed")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }
        } catch (Exception e) {
            context.getSource().getExecutor().sendMessage(
                Component.text("Error: " + e.getMessage())
                    .color(TextColor.color(0xFF5555))
            );
            return 0;
        }
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        try {
            if (!context.getSource().getExecutor().hasPermission("ecomonic.admin")) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("You don't have permission to use this command")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }
            
            Player target = context.getArgument("player", Player.class);
            List<Account> existing = Ecomonic.INSTANCE.getAccounts(target.getUniqueId());

            if (existing.size() >= Configurations.maxAccounts) {
                context.getSource().getExecutor().sendMessage(
                    Component.text("Player has reached maximum number of accounts")
                        .color(TextColor.color(0xFF5555))
                );
                return 0;
            }

            Account newAccount = Ecomonic.INSTANCE.createAccount(target.getUniqueId());
            if (existing.isEmpty()) {
                Ecomonic.INSTANCE.setPrimaryAccount(target.getUniqueId(), newAccount);
            }

            context.getSource().getExecutor().sendMessage(
                Component.text(String.format("Created account with ID %d for %s",
                    newAccount.getId(), target.getName()))
                    .color(TextColor.color(0x55FF55))
            );
            return 1;
        } catch (Exception e) {
            context.getSource().getExecutor().sendMessage(
                Component.text("Error: " + e.getMessage())
                    .color(TextColor.color(0xFF5555))
            );
            return 0;
        }
    }

    private int executeSet(CommandContext<CommandSourceStack> context) {
        try {
            if (!context.getSource().getExecutor().hasPermission("ecomonic.admin")) {
                context.getSource().getExecutor().sendMessage(Component.text("You don't have permission to use this command", TextColor.color(0xFF5555)));
                return 0;
            }

            long accountId = LongArgumentType.getLong(context, "accountId");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            Account account = Ecomonic.INSTANCE.getAccount(accountId);
            if (account == null) {
                context.getSource().getExecutor().sendMessage(Component.text("Account not found", TextColor.color(0xFF5555)));
                return 0;
            }

            if (account.set(amount)) {
                context.getSource().getExecutor().sendMessage(Component.text(String.format("Set balance of account %d to %.2f", accountId, amount), TextColor.color(0x55FF55)));
                return 1;
            } else {
                context.getSource().getExecutor().sendMessage(Component.text("Failed to set account balance", TextColor.color(0xFF5555)));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().getExecutor().sendMessage(Component.text("Error: " + e.getMessage()).color(TextColor.color(0xFF5555)));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        try {
            if (!context.getSource().getExecutor().hasPermission("ecomonic.admin")) {
                context.getSource().getExecutor().sendMessage(Component.text("You don't have permission to use this command", TextColor.color(0xFF5555)));
                return 0;
            }

            long accountId = LongArgumentType.getLong(context, "accountId");
            Account account = Ecomonic.INSTANCE.getAccount(accountId);

            if (account == null) {
                context.getSource().getExecutor().sendMessage(Component.text("Account not found", TextColor.color(0xFF5555)));
                return 0;
            }

            if (Ecomonic.INSTANCE.deleteAccount(accountId) != null) {
                context.getSource().getExecutor().sendMessage(Component.text("Account " + accountId + " has been deleted").color(TextColor.color(0x55FF55)));
                return 1;
            } else {
                context.getSource().getExecutor().sendMessage(Component.text("Failed to delete account", TextColor.color(0xFF5555)));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().getExecutor().sendMessage(Component.text("Error: " + e.getMessage()).color(TextColor.color(0xFF5555)));
            return 0;
        }
    }
}
