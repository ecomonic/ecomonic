package net.yellowstrawberry.ecomonic.data.sources;

import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.account.CachedAccount;
import net.yellowstrawberry.ecomonic.api.data.DataSource;
import net.yellowstrawberry.ecomonic.api.logging.Actions;
import net.yellowstrawberry.ecomonic.data.DatabaseType;
import net.yellowstrawberry.ecomonic.data.utils.DatabaseUtils;
import net.yellowstrawberry.ecomonic.data.utils.SQLUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PostgresDataSource implements DataSource {

    private SQLUtils sql;

    @Override
    public void connect(String host, Map<String, String> details) throws Exception {
        Class.forName("org.postgresql.Driver", true, DatabaseUtils.DATABASE_CLASS_LOADER);
        sql = new SQLUtils(DatabaseType.POSTGRESQL.name(), host, details.get("username"), details.get("password"), details.get("database"));
        sql.execute("""
        CREATE TABLE IF NOT EXISTS accounts (
            id BIGINT NOT NULL PRIMARY KEY,
            owner UUID,
            balance MONEY DEFAULT 0,
            FOREIGN KEY (owner) REFERENCES players(uuid) ON DELETE CASCADE
        );
        CREATE TABLE IF NOT EXISTS players (
            id UUID NOT NULL PRIMARY KEY,
            primary_account BIGINT,
            FOREIGN KEY (primary_account) REFERENCES accounts(id) ON DELETE SET NULL
        );
        CREATE TABLE IF NOT EXISTS logs (
            id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
            account_id INTEGER NOT NULL,
            action TEXT NOT NULL,
            amount REAL DEFAULT 0.0,
            actionee UUID,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (account_id) REFERENCES accounts(id)
        );
        """);
    }

    @Override
    public Account getAccount(long id, Class<? extends Account> clazz) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM accounts WHERE id = ? LIMIT 1;", id)){
            if (!set.next()) return null;
            UUID uuid = (UUID) set.getObject("owner");

            return clazz.isAssignableFrom(CachedAccount.class) ?
                    clazz.getConstructor(long.class, UUID.class, double.class).newInstance(id, uuid, set.getDouble("balance"))
                    : clazz.getConstructor(long.class, UUID.class).newInstance(id, uuid);
        } catch (SQLException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Account getPrimaryAccount(UUID uuid, Class<? extends Account> clazz) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM players WHERE id = ? LIMIT 1;", uuid)){
            return getAccount(set.getLong("primary_account"), clazz);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPrimaryAccount(UUID uuid, long l) {
        sql.execute("INSERT OR IGNORE INTO players (id, primary_account) VALUES (?, ?);", uuid, l);
        sql.execute("UPDATE players SET primary_account = ? WHERE id = ?;", l, uuid);
        try {
            sql.getConnection().commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull List<Long> getAccounts(UUID uuid) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM accounts WHERE owner = ?;", uuid)){
            List<Long> accounts = new ArrayList<>();
            while (set.next()) {
                long id = set.getLong("id");
                accounts.add(id);
            }

            return accounts;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull Account createAccount(long id, UUID uuid, Class<? extends Account> clazz) {
        sql.execute("INSERT INTO accounts (id, owner, balance) VALUES (?, ?, 0);", id, uuid);
        sql.execute("INSERT OR IGNORE INTO players (id, primary_account) VALUES (?, ?);", uuid, id);
        try {
            sql.getConnection().commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Objects.requireNonNull(getAccount(id, clazz));
    }

    @Override
    public void deleteAccount(long id) {
        sql.execute("DELETE FROM accounts WHERE id = ?;", id);
    }

    @Override
    public boolean withdraw(long id, double amount) {
        if (amount <= 0) return false;
        return sql.executeUpdate("UPDATE accounts SET balance = balance - ? WHERE id = ? AND ? <= balance;", amount, id, amount) != 0;
    }

    @Override
    public boolean deposit(long id, double amount) {
        if (amount <= 0) return false;
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
    public void close() {
        sql.close();
    }
}
