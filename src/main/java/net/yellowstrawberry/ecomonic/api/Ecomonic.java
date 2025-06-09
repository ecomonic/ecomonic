package net.yellowstrawberry.ecomonic.api;

import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.listener.AccountListener;
import net.yellowstrawberry.ecomonic.api.listener.EcomonicListener;

import java.util.List;
import java.util.UUID;

/**
 * The main interface for the Ecomonic API, providing methods to manage accounts and perform financial operations.
 * <p>
 * This interface allows for creating, deleting, and managing accounts, as well as performing transactions such as
 * deposits and withdrawals. It also supports event listeners for account changes and economic events.
 * </p>
 *
 * @since 0.0.1
 * @version 0.0.1
 */
public interface Ecomonic {
    /**
     * Returns the version of the Ecomonic API.
     *
     * @return the version string
     */
    String getVersion();

    /**
     * Returns the account with the specified ID.
     *
     * @param id the unique ID of the account
     * @return the account object, or null if not found
     */
    Account getAccount(Long id);

    /**
     * Returns the primary account for the specified UUID.
     *
     * @param uuid the UUID of the player
     * @return the primary account object, or null if not found
     */
    Account getPrimaryAccount(UUID uuid);

    /**
     * Returns a list of all accounts owned by the specified UUID.
     *
     * @param uuid the UUID of the player
     * @return a list of account objects
     */
    List<Account> getAccounts(UUID uuid);

    /**
     * Creates a new account for the specified UUID.
     *
     * @param uuid the UUID of the player
     * @return the created account object
     */
    Account createAccount(UUID uuid);

    /**
     * Deletes the account with the specified ID.
     *
     * @param id the unique ID of the account
     * @return the deleted account object, or null if not found
     */
    Account deleteAccount(long id);

    /**
     * Withdraws the specified amount from the account.
     *
     * @param id the unique ID of the account
     * @param amount the amount to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    boolean withdraw(long id, double amount);

    /**
     * Deposits the specified amount into the account.
     *
     * @param id the unique ID of the account
     * @param amount the amount to deposit
     * @return true if the deposit was successful, false otherwise
     */
    boolean deposit(long id, double amount);

    /**
     * Sets the balance of the account to the specified amount.
     *
     * @param id the unique ID of the account
     * @param amount the new balance to set
     * @return true if the balance was set successfully, false otherwise
     */
    boolean set(long id, double amount);

    /**
     * Checks if the account has at least the specified amount.
     *
     * @param id the unique ID of the account
     * @param amount the amount to check
     * @return true if the account has at least the specified amount, false otherwise
     */
    boolean has(long id, double amount);

    /**
     * Returns the balance of the specified account.
     *
     * @param id the unique ID of the account
     * @return the account balance
     */
    double getBalance(Long id);

    /**
     * Registers an Ecomonic event listener.
     *
     * @param listener the event listener to register
     */
    void addEventListener(EcomonicListener listener);

    /**
     * Unregisters an Ecomonic event listener.
     *
     * @param listener the event listener to unregister
     */
    void removeEventListener(EcomonicListener listener);

    /**
     * Registers an account listener for a specific account.
     *
     * @param id the unique ID of the account
     * @param listener the account listener to register
     */
    void addAccountListener(long id, AccountListener listener);

    /**
     * Unregisters an account listener for a specific account.
     *
     * @param id the unique ID of the account
     * @param listener the account listener to unregister
     */
    void removeAccountListener(long id, AccountListener listener);
}
