package net.yellowstrawberry.ecomonic.api.account;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Represents a financial account in the system.
 * <p>
 * Provides methods for managing the account's balance, including depositing, withdrawing,
 * setting, and checking money. Each account is associated with an owner (UUID) and has a unique identifier (ID).
 * </p>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
public interface Account {

    /**
     * Returns the unique identifier of the account owner.
     *
     * @return the UUID of the account owner
     */
    @Nullable
    UUID getOwner();

    /**
     * Returns the unique ID of the account.
     *
     * @return the account ID
     */
    @Nonnull
    Long getId();

    /**
     * Attempts to withdraw the specified amount from the account.
     *
     * @param amount the amount to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    boolean withdraw(double amount);

    /**
     * Attempts to deposit the specified amount into the account.
     *
     * @param amount the amount to deposit
     * @return true if the deposit was successful, false otherwise
     */
    boolean deposit(double amount);

    /**
     * Sets the account balance to the specified amount.
     *
     * @param amount the new balance to set
     * @return true if the balance was set successfully, false otherwise
     */
    boolean set(double amount);

    /**
     * Checks if the account has at least the specified amount.
     *
     * @param amount the amount to check
     * @return true if the account has at least the specified amount, false otherwise
     */
    boolean has(double amount);

    /**
     * Returns the current balance of the account.
     *
     * @return the account balance
     */
    double getBalance();
}
