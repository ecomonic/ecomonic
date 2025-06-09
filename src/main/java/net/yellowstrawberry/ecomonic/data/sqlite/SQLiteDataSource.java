package net.yellowstrawberry.ecomonic.data.sqlite;

import io.hypersistence.tsid.TSID;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.account.CachedAccount;
import net.yellowstrawberry.ecomonic.api.account.SyncedAccount;
import net.yellowstrawberry.ecomonic.api.logging.Actions;
import net.yellowstrawberry.ecomonic.config.Configurations;
import net.yellowstrawberry.ecomonic.data.DataSource;
import net.yellowstrawberry.ecomonic.data.utils.SQLUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLiteDataSource implements DataSource {

    private SQLUtils sql;
    private HashMap<UUID, Account> primaryAccounts;
    private Long2ObjectMap<Account> cachedAccounts;

    @Override
    public void connect(String host, Map<String, String> details) throws Exception {
        Class.forName("org.sqlite.JDBC");
        sql = new SQLUtils("jdbc:sqlite:"+host);
        sql.execute("""
        CREATE TABLE IF NOT EXISTS accounts (
            id INTEGER NOT NULL PRIMARY KEY,
            owner BLOB,
            balance REAL DEFAULT 0.0,
            FOREIGN KEY (owner) REFERENCES players(uuid)
        );
        CREATE TABLE IF NOT EXISTS players (
            id BLOB NOT NULL PRIMARY KEY,
            primary_account INTEGER,
            FOREIGN KEY (primary_account) REFERENCES accounts(id)
        );
        CREATE TABLE IF NOT EXISTS logs (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            account_id INTEGER NOT NULL,
            action TEXT NOT NULL,
            amount REAL DEFAULT 0.0,
            actionee BLOB,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (account_id) REFERENCES accounts(id)
        );
        """);

        if (Configurations.cache) {
            primaryAccounts = new HashMap<>();
            cachedAccounts = new Long2ObjectOpenHashMap<>();
        }else {
            primaryAccounts = null;
            cachedAccounts = null;
        }
    }

    @Override
    public Account getAccount(long id) {
        if (cachedAccounts != null && cachedAccounts.containsKey(id)) return cachedAccounts.get(id);
        try (ResultSet set = sql.executeQuery("SELECT * FROM accounts WHERE id = ? LIMIT 1;", id)){
            if (!set.first()) return null;

            UUID uuid = set.getObject("owner", UUID.class);

            return getAccount(uuid, set, id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Account getPrimaryAccount(UUID uuid) {
        if (primaryAccounts != null && primaryAccounts.containsKey(uuid)) return primaryAccounts.get(uuid);
        try (ResultSet set = sql.executeQuery("SELECT * FROM players WHERE id = ? LIMIT 1;", uuid)){
            return getAccount(set.getLong("primary_account"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull List<Account> getAccounts(UUID uuid) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM accounts WHERE owner = ?;", uuid)){
            List<Account> accounts = new ArrayList<>();
            while (set.next()) {
                long id = set.getLong("id");
                if (cachedAccounts != null && cachedAccounts.containsKey(id)) {
                    accounts.add(cachedAccounts.get(id));
                    continue; // Skip if already cached
                }

                accounts.add(getAccount(uuid, set, id));
            }

            return accounts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private Account getAccount(UUID uuid, ResultSet set, long id) throws SQLException {
        Account a;
        if (cachedAccounts != null && primaryAccounts != null) {
            a = new CachedAccount(id, uuid, set.getDouble("balance"));
            cachedAccounts.put(id, a);
            primaryAccounts.put(uuid, a);
        } else {
            a = new SyncedAccount(id, uuid);
        }
        return a;
    }

    @Override
    public Account createAccount(UUID uuid) {
        long id = TSID.fast().toLong();
        sql.execute("INSERT INTO accounts (id, owner, balance) VALUES (?, ?, 0);", id, uuid);
        sql.execute("INSERT OR IGNORE INTO players (id, primary_account) VALUES (?, ?);", uuid, id);
        return Objects.requireNonNull(getAccount(id));
    }

    @Override
    public Account deleteAccount(long id) {
        Account acc = getAccount(id);
        if (acc == null) return null;
        sql.execute("DELETE FROM accounts WHERE id = ?;", id);
        if (cachedAccounts != null) cachedAccounts.remove(id);
        if (primaryAccounts != null && acc != null) primaryAccounts.values().remove(acc);
        return acc;
    }

    @Override
    public boolean withdraw(long id, double amount) {
        double balance = getBalance(id);
        if (balance < amount) return false;
        return sql.executeUpdate("UPDATE accounts SET balance = balance - ? WHERE id = ? AND ? <= balance;", amount, id, amount) != 0;
    }

    @Override
    public boolean deposit(long id, double amount) {
        return sql.executeUpdate("UPDATE accounts SET balance = balance + ? WHERE id = ?;", amount, id) != 0;
    }

    @Override
    public boolean set(long id, double amount) {
        return sql.executeUpdate("UPDATE accounts SET balance = ? WHERE id = ?;", amount, id) != 0;
    }

    @Override
    public boolean has(long id, double amount) {
        try (ResultSet set = sql.executeQuery("SELECT 1 FROM accounts WHERE id = ? AND ? <= balance;", id, amount)) {
            return set.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getBalance(Long id) {
        try (ResultSet set = sql.executeQuery("SELECT balance FROM accounts WHERE id = ?;", id)) {
            if (set.next()) return set.getDouble("balance");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    @Override
    public void log(Long accountId, Actions action, Double amount, UUID actionee) {
        sql.execute(
            "INSERT INTO logs (account_id, action, amount, actionee) VALUES (?, ?, ?, ?);",
            accountId,
            action.name(),
            amount != null ? amount : 0.0,
            actionee
        );
    }

    @Override
    public void close() throws SQLException {
        sql.close();
    }
}
