package net.yellowstrawberry.ecomonic.api.account;

import net.yellowstrawberry.ecomonic.EcomonicPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a cached account in the system.
 * <p>
 * When using this account, it will use the cached data.
 * This is useful for accounts that are not needed to reflect real-time changes.
 * </p>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
public class CachedAccount implements Account {

    private final Long id;
    private final UUID owner;
    private double balance;

    public CachedAccount(Long id, UUID owner, double balance) {
        this.id = id;
        this.owner = owner;
        this.balance = balance;
    }

    public UUID getOwner() {
        return owner;
    }

    public @NotNull Long getId() {
        return id;
    }

    public synchronized boolean withdraw(double amount) {
        if (amount <= 0 || !has(amount)) return false;
        balance -= amount;
        return true;
    }

    public synchronized boolean deposit(double amount) {
        if (amount <= 0) return false;
        balance += amount;
        return true;
    }

    public synchronized boolean set(double amount) {
        if (amount < 0) return false;
        balance = amount;
        return true;
    }

    public boolean has(double amount) {
        return amount <= balance;
    }

    public double getBalance() {
        return balance;
    }
}