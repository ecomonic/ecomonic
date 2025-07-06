package net.yellowstrawberry.ecomonic.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.yellowstrawberry.ecomonic.api.Ecomonic;
import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.config.Configurations;
import net.yellowstrawberry.ecomonic.translation.Translator;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class AccountCommand implements CommandRoot {
    @Override
    public LiteralCommandNode<CommandSourceStack> build() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("account")
                .then(buildListCommand())
                .then(buildCreateCommand())
                .then(buildPrimaryCommand())
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
                Translator.send(context, "ecomonic.general.error.not_a_player");
                return 0;
            }

            List<Account> existing = Ecomonic.INSTANCE.getAccounts(player.getUniqueId());
            if (existing.size() >= Configurations.maxAccounts) {
                Translator.send(context, "ecomonic.account.error.max_accounts.self", Map.of("player", player.getName()));
                return 0;
            }

            Account newAccount = Ecomonic.INSTANCE.createAccount(player.getUniqueId());
            if (existing.isEmpty()) {
                Ecomonic.INSTANCE.setPrimaryAccount(player.getUniqueId(), newAccount);
            }

            Translator.send(context, "ecomonic.account.created.self", Map.of("id", newAccount.getId()));
            return 1;
        } catch (Exception e) {
            Translator.send(context, "ecomonic.general.error.unexpected_error", Map.of("msg", e.getMessage()));
            return 0;
        }
    }

    private int executePrimary(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getExecutor() instanceof Player ? (Player) context.getSource().getExecutor() : null;
        if (player == null) {
            Translator.send(context, "ecomonic.general.error.not_a_player");
            return 0;
        }

        long accountId = LongArgumentType.getLong(context, "accountId");
        Account account = Ecomonic.INSTANCE.getAccount(accountId);

        if (account == null) {
            Translator.send(context, "ecomonic.account.error.not_found");
            return 0;
        }

        Ecomonic.INSTANCE.setPrimaryAccount(player.getUniqueId(), account);
        Translator.send(context, "ecomonic.account.primary", Map.of("player", player.getName(), "id", accountId));

        return 1;
    }

    private int executeDeleteSelf(CommandContext<CommandSourceStack> context) {
        try {
            Player player = context.getSource().getExecutor() instanceof Player ? (Player) context.getSource().getExecutor() : null;
            if (player == null) {
                Translator.send(context, "ecomonic.general.error.not_a_player");
                return 0;
            }


            long accountId = LongArgumentType.getLong(context, "accountId");
            Account account = Ecomonic.INSTANCE.getAccount(accountId);

            if (account == null) {
                Translator.send(context, "ecomonic.account.error.not_found");
                return 0;
            }

            if (!player.hasPermission("ecomonic.admin") && !player.getUniqueId().equals(account.getOwner())) {
                Translator.send(context, "ecomonic.account.error.not_a_owner", Map.of("player", player.getName()));
                return 0;
            }

            if (Ecomonic.INSTANCE.getAccounts(player.getUniqueId()).size() <= 1 &&
                    Ecomonic.INSTANCE.getPrimaryAccount(player.getUniqueId()).getId() == accountId) {
                Translator.send(context, "ecomonic.account.error.only_account", Map.of("player", player.getName()));
                return 0;
            }

            if (account.getBalance() > 0) {
                Translator.send(context, "ecomonic.account.error.non_zero", Map.of("player", player.getName()));
                return 0;
            }

            Account newPrimary = Ecomonic.INSTANCE.getAccounts(player.getUniqueId()).getFirst();
            if (Ecomonic.INSTANCE.getPrimaryAccount(player.getUniqueId()).getId() == accountId) {
                Ecomonic.INSTANCE.setPrimaryAccount(player.getUniqueId(), newPrimary);
                Translator.send(context, "ecomonic.account.deleted.self.primary", Map.of("id", accountId, "primary", newPrimary.getId()));
            }else {
                Translator.send(context, "ecomonic.account.deleted.self.normal", Map.of("id", accountId));
            }

            return 1;
        } catch (Exception e) {
            Translator.send(context, "ecomonic.general.error.unexpected_error", Map.of("msg", e.getMessage()));
            return 0;
        }
    }

    private int executeList(CommandContext<CommandSourceStack> context) {
        try {
            Player player = context.getSource().getExecutor() instanceof Player ? (Player) context.getSource().getExecutor() : null;
            if (player == null) {
                Translator.send(context, "ecomonic.general.error.not_a_player");
                return 0;
            }

            List<Account> accounts = Ecomonic.INSTANCE.getAccounts(player.getUniqueId());
            
            if (accounts.isEmpty()) {
                Translator.send(context, "ecomonic.account.error.no_accounts");
                return 0;
            }

            context.getSource().getExecutor().sendMessage(Component.text("Your accounts:"));
            Account primaryAccount = Ecomonic.INSTANCE.getPrimaryAccount(player.getUniqueId());
            for (Account account : accounts) {
                boolean isPrimary = primaryAccount != null && primaryAccount.getId() == account.getId();
                context.getSource().getExecutor().sendMessage(
                        Component.textOfChildren(
                                Component.text("ID: "),
                                Component.text(account.getId())
                                        .hoverEvent(HoverEvent.showText(Component.text("Click to copy")))
                                        .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, String.valueOf(account.getId()))),
                                Component.text(", "),
                                Component.text("Balance: %.2f%s".formatted(account.getBalance(), isPrimary ? " (Primary)" : ""))
                        ).color(TextColor.color(0x55FF55))
                );
            }
            return 1;
        } catch (Exception e) {
            Translator.send(context, "ecomonic.general.error.unexpected_error", Map.of("msg", e.getMessage()));
            return 0;
        }
    }

    private int executeTransfer(CommandContext<CommandSourceStack> context) {
        try {
            Player player = context.getSource().getExecutor() instanceof Player ?
                    (Player) context.getSource().getExecutor() : null;
            if (player == null) {
                Translator.send(context, "ecomonic.general.error.not_a_player");
                return 0;
            }

            long fromId = LongArgumentType.getLong(context, "from");
            long toId = LongArgumentType.getLong(context, "to");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            if (amount < Configurations.minTransactionAmount) {
                Translator.send(context, "ecomonic.account.error.below_min", Map.of("min", Configurations.minTransactionAmount));
                return 0;
            }

            if (amount > Configurations.maxTransactionAmount) {
                Translator.send(context, "ecomonic.account.error.above_max", Map.of("max", Configurations.maxTransactionAmount));
                return 0;
            }

            Account fromAccount = Ecomonic.INSTANCE.getAccount(fromId);
            Account toAccount = Ecomonic.INSTANCE.getAccount(toId);

            if (fromAccount == null || toAccount == null) {
                Translator.send(context, "ecomonic.account.error.non_found");
                return 0;
            }

            if (fromAccount.getId() == toAccount.getId()) {
                Translator.send(context, "ecomonic.account.error.same_account");
                return 0;
            }

            if (!player.getUniqueId().equals(fromAccount.getOwner())) {
                Translator.send(context, "ecomonic.account.error.not_a_source_owner");
                return 0;
            }

            double totalAmount = amount;
            double fee = 0;
            if (Configurations.enableTransactionFees) {
                fee = Math.round(amount * (Configurations.transactionFeePercentage / 100.0) * 100.0) / 100.0;
                totalAmount += fee;
            }

            if (!fromAccount.has(totalAmount)) {
                Translator.send(context, "ecomonic.account.error.not_enough", Map.of("amount", amount, "fee", "%.2f".formatted(fee)));
                return 0;
            }

            if (fromAccount.withdraw(totalAmount) && toAccount.deposit(amount)) {
                Translator.send(context, "ecomonic.account.transfer", Map.of(
                        "amount", amount,
                        "from", fromId,
                        "to", toId,
                        "fee", fee
                ));
                return 1;
            } else {
                Translator.send(context, "ecomonic.account.error.failed_to_transfer");
                return 0;
            }
        } catch (Exception e) {
            Translator.send(context, "ecomonic.general.error.unexpected_error", Map.of("msg", e.getMessage()));
            return 0;
        }
    }

    private int executeCreate(CommandContext<CommandSourceStack> context) {
        try {
            if (!context.getSource().getExecutor().hasPermission("ecomonic.admin")) {
                Translator.send(context, "ecomonic.general.error.permission");
                return 0;
            }
            
            Player target = context.getArgument("player", Player.class);
            List<Account> existing = Ecomonic.INSTANCE.getAccounts(target.getUniqueId());

            if (existing.size() >= Configurations.maxAccounts) {
                Translator.send(context, "ecomonic.account.error.max_accounts.admin", Map.of("player", target.getName()));
                return 0;
            }

            Account newAccount = Ecomonic.INSTANCE.createAccount(target.getUniqueId());
            if (existing.isEmpty()) {
                Ecomonic.INSTANCE.setPrimaryAccount(target.getUniqueId(), newAccount);
            }

            Translator.send(context, "ecomonic.account.created.admin", Map.of("id", newAccount.getId(), "target", target.getName()));
            return 1;
        } catch (Exception e) {
            Translator.send(context, "ecomonic.general.error.unexpected_error", Map.of("msg", e.getMessage()));
            return 0;
        }
    }

    private int executeSet(CommandContext<CommandSourceStack> context) {
        try {
            if (!context.getSource().getExecutor().hasPermission("ecomonic.admin")) {
                Translator.send(context, "ecomonic.general.error.permission");
                return 0;
            }

            long accountId = LongArgumentType.getLong(context, "accountId");
            double amount = DoubleArgumentType.getDouble(context, "amount");

            Account account = Ecomonic.INSTANCE.getAccount(accountId);
            if (account == null) {
                Translator.send(context, "ecomonic.account.error.not_found");
                return 0;
            }

            if (account.set(amount)) {
                Translator.send(context, "ecomonic.account.set", Map.of("id", accountId, "amount", amount));
                return 1;
            } else {
                Translator.send(context, "ecomonic.account.error.failed_to_set");
                return 0;
            }
        } catch (Exception e) {
            Translator.send(context, "ecomonic.general.error.unexpected_error", Map.of("msg", e.getMessage()));
            return 0;
        }
    }

    private int executeDelete(CommandContext<CommandSourceStack> context) {
        try {
            if (!context.getSource().getSender().hasPermission("ecomonic.admin")) {
                Translator.send(context, "ecomonic.general.error.permission");
                return 0;
            }

            long accountId = LongArgumentType.getLong(context, "accountId");
            Account account = Ecomonic.INSTANCE.getAccount(accountId);

            if (account == null) {
                Translator.send(context, "ecomonic.account.error.not_found");
                return 0;
            }

            Translator.send(context, "ecomonic.account.deleted.admin", Map.of("id", accountId));
            return 1;
        } catch (Exception e) {
            Translator.send(context, "ecomonic.general.error.unexpected_error", Map.of("msg", e.getMessage()));
            return 0;
        }
    }
}
