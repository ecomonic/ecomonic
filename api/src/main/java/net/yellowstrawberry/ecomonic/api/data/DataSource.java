package net.yellowstrawberry.ecomonic.api.data;

import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.logging.Actions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for data source implementations that handle account data storage and retrieval.
 * Provides methods for connecting, managing accounts, and performing transactions.
 *
 * @version 0.0.1
 * @since 0.0.1
 */
public interface DataSource {
    /**
     * Establishes a connection to the data source.
     *
     * @throws Exception if a connection error occurs
     */
    void connect(String host, Map<String, String> details) throws Exception;

    /**
     * Retrieves the account with the specified ID.
     *
     * @param id the unique ID of the account
     * @param clazz the class type of the account to retrieve
     * @return the account object, or null if not found
     */
    @Nullable
    Account getAccount(long id, Class<? extends Account> clazz);

    /**
     * Retrieves the primary account for the specified UUID.
     *
     * @param uuid the UUID of the player
     * @param clazz the class type of the account to retrieve
     * @return the primary account object, or null if not found
     */
    @Nullable
    Account getPrimaryAccount(UUID uuid, Class<? extends Account> clazz);

    /**
     * Retrieves the primary account for the specified UUID.
     *
     * @param uuid the UUID of the player
     * @param id the unique ID of the account
     */
    void setPrimaryAccount(UUID uuid, long id);

    /**
     * Retrieves all accounts owned by the specified UUID.
     *
     * @param uuid the UUID of the player
     * @return a list of account objects (never null)
     */
    @NotNull
    List<Long> getAccounts(UUID uuid);

    /**
     * Creates a new account for the specified UUID.
     *
     * @param uuid the UUID of the player
     * @param clazz the class type of the account to retrieve
     * @return the created account object (never null)
     */
    @NotNull
    Account createAccount(long id, UUID uuid, Class<? extends Account> clazz);

    /**
     * Deletes the account with the specified ID.
     *
     * @param id the unique ID of the account
     */
    void deleteAccount(long id);

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
     * Logs an action performed on an account.
     *
     * @param accountId the unique ID of the account
     * @param action the action performed
     * @param amount the amount involved in the action, if applicable
     * @param actionee the UUID of the player who performed the action, or null if not applicable
     */
    void log(Long accountId, Actions action, Double amount, UUID actionee);

    /**
     * Closes the connection to the data source and releases any resources.
     *
     * @throws Exception if an error occurs while closing
     */
    void close() throws Exception;
}
