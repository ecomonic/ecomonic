package net.yellowstrawberry.ecomonic.data.sources;

import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.account.CachedAccount;
import net.yellowstrawberry.ecomonic.api.logging.Actions;
import net.yellowstrawberry.ecomonic.api.data.DataSource;
import net.yellowstrawberry.ecomonic.config.Configurations;
import net.yellowstrawberry.ecomonic.data.utils.SQLUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLiteDataSource implements DataSource {

    private SQLUtils sql;

    @Override
    public void connect(String host, Map<String, String> details) {
        sql = new SQLUtils("jdbc:sqlite:"+ Path.of(Configurations.root.toURI()).resolve(host));
        System.out.println("jdbc:sqlite:"+Path.of(Configurations.root.toURI()).resolve(host));
        sql.executeUpdate("""
        CREATE TABLE IF NOT EXISTS accounts (
            id INTEGER NOT NULL PRIMARY KEY,
            owner BLOB,
            balance REAL DEFAULT 0.0,
            FOREIGN KEY (owner) REFERENCES players(id)
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
    }

    @Override
    public Account getAccount(long id, Class<? extends Account> clazz) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM accounts WHERE id = ? LIMIT 1;", id)){
            if (!set.next()) return null;
            UUID uuid = asUuid(set.getBytes("owner"));

            return clazz.getConstructor(long.class, UUID.class).newInstance(id, uuid);
        } catch (SQLException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Account getPrimaryAccount(UUID uuid, Class<? extends Account> clazz) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM players WHERE id = ? LIMIT 1;", asStream(uuid))){
            if (!set.next()) return null;
            return getAccount(set.getLong("primary_account"), clazz);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPrimaryAccount(UUID uuid, long l) {
        sql.execute("INSERT OR IGNORE INTO players (id, primary_account) VALUES (?, ?);", asStream(uuid), l);
        sql.execute("UPDATE players SET primary_account = ? WHERE id = ?;", l, asStream(uuid));
    }

    @Override
    public @NotNull List<Long> getAccounts(UUID uuid) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM accounts WHERE owner = ?;", asStream(uuid))){
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
        sql.execute("INSERT INTO accounts (id, owner, balance) VALUES (?, ?, 0);", id, asStream(uuid));
        sql.execute("INSERT OR IGNORE INTO players (id, primary_account) VALUES (?, ?);", asStream(uuid), id);
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
            asStream(actionee)
        );
    }

    @Override
    public void close() throws SQLException {
        sql.close();
    }

    public static UUID asUuid(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static InputStream asStream(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return new ByteArrayInputStream(bb.array());
    }
}
