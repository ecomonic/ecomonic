package net.yellowstrawberry.ecomonic.api.account;

import net.yellowstrawberry.ecomonic.EcomonicPlugin;
import net.yellowstrawberry.ecomonic.api.Ecomonic;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a datasource based account in the system.
 * <p>
 * When using this account, it will always fetch the latest data from the datasource.
 * This is useful for accounts that are needed to reflect real-time changes.
 * </p>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
public class SyncedAccount implements Account {

    private final Long id;
    private final UUID owner;

    public SyncedAccount(Long id, UUID owner) {
        this.id = id;
        this.owner = owner;
    }

    public UUID getOwner() {
        return owner;
    }

    public @NotNull Long getId() {
        return id;
    }

    public boolean withdraw(double amount) {
        if (amount <= 0) return false;
        return Ecomonic.INSTANCE.withdraw(getId(), amount);
    }

    public boolean deposit(double amount) {
        return Ecomonic.INSTANCE.deposit(getId(), amount);
    }

    public boolean set(double amount) {
        return Ecomonic.INSTANCE.set(getId(), amount);
    }

    public boolean has(double amount) {
        return Ecomonic.INSTANCE.has(getId(), amount);
    }

    public double getBalance() {
        return Ecomonic.INSTANCE.getBalance(getId());
    }
}
