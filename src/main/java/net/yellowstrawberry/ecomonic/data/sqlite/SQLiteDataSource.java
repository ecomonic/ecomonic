package net.yellowstrawberry.ecomonic.data.sqlite;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.account.ServerAccount;
import net.yellowstrawberry.ecomonic.api.logging.Actions;
import net.yellowstrawberry.ecomonic.data.DataSource;
import net.yellowstrawberry.ecomonic.data.utils.SQLUtils;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SQLiteDataSource implements DataSource {

    private SQLUtils sql;
    private final HashMap<UUID, Long> uuidToId = new HashMap<>();
    private final Long2ObjectMap<Account> cachedAccounts = new Long2ObjectOpenHashMap<>();

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
            uuid BLOB NOT NULL PRIMARY KEY,
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
    public Account getAccount(long id) {
        if (cachedAccounts != null && cachedAccounts.containsKey(id)) return cachedAccounts.get(id);
        try (ResultSet set = sql.executeQuery("SELECT * FROM accounts WHERE id = ? LIMIT 1;", id)){
            if (!set.first()) return null;

            Account a;

            UUID uuid = set.getObject("owner", UUID.class);
            if (uuid == null) a = new ServerAccount(id);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    @Override
    public Account getPrimaryAccount(UUID uuid) {
        return null;
    }

    @Override
    public @NotNull List<Account> getAccounts(UUID uuid) {
        return List.of();
    }

    @Override
    public Account createAccount(UUID uuid) {
        return null;
    }

    @Override
    public Account deleteAccount(long id) {
        return null;
    }

    @Override
    public boolean withdraw(long id, double amount) {
        return false;
    }

    @Override
    public boolean deposit(long id, double amount) {
        return false;
    }

    @Override
    public boolean set(long id, double amount) {
        return false;
    }

    @Override
    public boolean has(long id, double amount) {
        return false;
    }

    @Override
    public double getBalance(Long id) {
        return 0;
    }

    @Override
    public void log(Long accountId, Actions action, Double amount, UUID actionee) {

    }

    @Override
    public void close() throws SQLException {
        sql.close();
    }
}
