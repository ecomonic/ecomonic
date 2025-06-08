package net.yellowstrawberry.ecomonic.api.account;

import net.yellowstrawberry.ecomonic.EcomonicPlugin;

import java.util.UUID;

/**
 * Represents a financial account in the system.
 * <p>
 * Provides methods for managing the account's balance, including depositing, withdrawing,
 * setting, and checking funds. Each account is associated with an owner (UUID) and has a unique identifier (ID).
 * </p>
 */
public abstract class Account {
    /**
     * Returns the unique identifier of the account owner.
     *
     * @return the UUID of the account owner
     */
    abstract UUID getOwner();

    /**
     * Returns the unique ID of the account.
     *
     * @return the account ID
     */
    public abstract Long getId();

    /**
     * Attempts to withdraw the specified amount from the account.
     *
     * @param amount the amount to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(double amount) {
        return EcomonicPlugin.ecomonic.withdraw(getId(), amount);
    }

    /**
     * Attempts to deposit the specified amount into the account.
     *
     * @param amount the amount to deposit
     * @return true if the deposit was successful, false otherwise
     */
    public boolean deposit(double amount) {
        return EcomonicPlugin.ecomonic.deposit(getId(), amount);
    }

    /**
     * Sets the account balance to the specified amount.
     *
     * @param amount the new balance to set
     * @return true if the balance was set successfully, false otherwise
     */
    public boolean set(double amount) {
        return EcomonicPlugin.ecomonic.set(getId(), amount);
    }

    /**
     * Checks if the account has at least the specified amount.
     *
     * @param amount the amount to check
     * @return true if the account has at least the specified amount, false otherwise
     */
    public boolean has(double amount) {
        return EcomonicPlugin.ecomonic.has(getId(), amount);
    }

    /**
     * Returns the current balance of the account.
     *
     * @return the account balance
     */
    public double getBalance() {
        return EcomonicPlugin.ecomonic.getBalance(getId());
    }
}
