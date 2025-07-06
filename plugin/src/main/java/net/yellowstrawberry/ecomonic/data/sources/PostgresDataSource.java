package net.yellowstrawberry.ecomonic.data.sources;

import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.account.CachedAccount;
import net.yellowstrawberry.ecomonic.api.data.DataSource;
import net.yellowstrawberry.ecomonic.api.logging.Actions;
import net.yellowstrawberry.ecomonic.data.DatabaseType;
import net.yellowstrawberry.ecomonic.data.utils.SQLUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PostgresDataSource implements DataSource {

    private SQLUtils sql;

    @Override
    public void connect(String host, Map<String, String> details) {
        sql = new SQLUtils(DatabaseType.POSTGRESQL.getName(), host+(details.containsKey("port")?":"+details.get("port"):""), details.get("username"), details.get("password"), details.get("database"));
        sql.executeUpdate("CREATE SCHEMA IF NOT EXISTS ecomonic;");
        sql.executeUpdate("GRANT USAGE ON SCHEMA ecomonic TO %s;".formatted(details.get("username")));
        sql.executeUpdate("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA ecomonic TO %s;".formatted(details.get("username")));
        sql.executeUpdate("""
        CREATE TABLE IF NOT EXISTS ecomonic.accounts (
            id BIGINT NOT NULL PRIMARY KEY,
            owner UUID,
            balance MONEY DEFAULT 0
        );
        
        CREATE TABLE IF NOT EXISTS ecomonic.players (
            id UUID NOT NULL PRIMARY KEY,
            primary_account BIGINT,
            FOREIGN KEY (primary_account) REFERENCES ecomonic.accounts(id) ON DELETE SET NULL
        );
        
        CREATE TABLE IF NOT EXISTS ecomonic.logs (
            id SERIAL PRIMARY KEY,
            account_id BIGINT NOT NULL,
            action VARCHAR(255) NOT NULL,
            amount MONEY DEFAULT 0,
            actionee UUID,
            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (account_id) REFERENCES ecomonic.accounts(id)
        );
        """);
        sql.executeUpdate("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1
                        FROM pg_constraint
                        WHERE conname = 'fk_owner_uuid'
                    ) THEN
                        ALTER TABLE ecomonic.accounts
                        ADD CONSTRAINT fk_owner_uuid FOREIGN KEY (owner) REFERENCES ecomonic.players(id) ON DELETE CASCADE;
                    END IF;
                END
                $$;""");
    }

    @Override
    public Account getAccount(long id, Class<? extends Account> clazz) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM ecomonic.accounts WHERE id = ? LIMIT 1;", id)){
            if (!set.next()) return null;
            UUID uuid = (UUID) set.getObject("owner");

            return clazz.getConstructor(long.class, UUID.class).newInstance(id, uuid);
        } catch (SQLException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Account getPrimaryAccount(UUID uuid, Class<? extends Account> clazz) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM ecomonic.players WHERE id = ? LIMIT 1;", uuid)){
            if (!set.next()) return null;
            return getAccount(set.getLong("primary_account"), clazz);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setPrimaryAccount(UUID uuid, long l) {
        sql.execute("INSERT OR IGNORE INTO ecomonic.players (id, primary_account) VALUES (?, ?);", uuid, l);
        sql.execute("UPDATE players SET primary_account = ? WHERE id = ?;", l, uuid);
        try {
            sql.getConnection().commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NotNull List<Long> getAccounts(UUID uuid) {
        try (ResultSet set = sql.executeQuery("SELECT * FROM ecomonic.accounts WHERE owner = ?;", uuid)){
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
        sql.executeUpdate("INSERT INTO ecomonic.players (id, primary_account) VALUES (?, NULL) ON CONFLICT DO NOTHING;", uuid);
        sql.executeUpdate("INSERT INTO ecomonic.accounts (id, owner, balance) VALUES (?, ?, 0);", id, uuid);
        sql.executeUpdate("UPDATE ecomonic.players SET primary_account = ? WHERE id = ?;", id, uuid);
        return Objects.requireNonNull(getAccount(id, clazz));
    }

    @Override
    public void deleteAccount(long id) {
        sql.execute("DELETE FROM ecomonic.accounts WHERE id = ?;", id);
    }

    @Override
    public boolean withdraw(long id, double amount) {
        if (amount <= 0) return false;
        return sql.executeUpdate("UPDATE ecomonic.accounts SET balance = balance - ?::money WHERE id = ? AND ?::money <= balance;", String.valueOf(amount), id, String.valueOf(amount)) != 0;
    }

    @Override
    public boolean deposit(long id, double amount) {
        if (amount <= 0) return false;
        return sql.executeUpdate("UPDATE ecomonic.accounts SET balance = balance + ?::money WHERE id = ?;", String.valueOf(amount), id) != 0;
    }

    @Override
    public boolean set(long id, double amount) {
        return sql.executeUpdate("UPDATE ecomonic.accounts SET balance = ?::money WHERE id = ?;", String.valueOf(amount), id) != 0;
    }

    @Override
    public boolean has(long id, double amount) {
        try (ResultSet set = sql.executeQuery("SELECT 1 FROM ecomonic.accounts WHERE id = ? AND ?::money <= balance;", id, String.valueOf(amount))) {
            return set.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getBalance(Long id) {
        try (ResultSet set = sql.executeQuery("SELECT balance FROM ecomonic.accounts WHERE id = ?;", id)) {
            if (set.next()) return set.getDouble("balance");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    @Override
    public void log(Long accountId, Actions action, Double amount, UUID actionee) {
        sql.execute(
            "INSERT INTO ecomonic.logs (account_id, action, amount, actionee) VALUES (?, ?, ?, ?);",
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
