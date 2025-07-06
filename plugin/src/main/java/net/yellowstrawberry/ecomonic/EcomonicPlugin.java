package net.yellowstrawberry.ecomonic;

import io.hypersistence.tsid.TSID;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.yellowstrawberry.ecomonic.api.Ecomonic;
import net.yellowstrawberry.ecomonic.api.account.Account;
import net.yellowstrawberry.ecomonic.api.account.CachedAccount;
import net.yellowstrawberry.ecomonic.api.account.SyncedAccount;
import net.yellowstrawberry.ecomonic.api.data.DataSource;
import net.yellowstrawberry.ecomonic.api.listener.AccountListener;
import net.yellowstrawberry.ecomonic.api.listener.EcomonicListener;
import net.yellowstrawberry.ecomonic.command.CommandRegistrar;
import net.yellowstrawberry.ecomonic.config.Configurations;
import net.yellowstrawberry.ecomonic.data.DatabaseType;
import net.yellowstrawberry.ecomonic.data.sqlite.SQLiteDataSource;
import net.yellowstrawberry.ecomonic.data.utils.DatabaseUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class EcomonicPlugin extends JavaPlugin implements Ecomonic, Listener {

    public static EcomonicPlugin plugin;
    private final long loadedTime = System.currentTimeMillis();

    private static DataSource source;
    private HashMap<UUID, Account> primaryAccounts = new HashMap<>();
    private Long2ObjectMap<Account> cachedAccounts = new Long2ObjectOpenHashMap<>();
    /**
     * Plugin
     * */

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onEnable() {
        plugin = this;

        Configurations.load();
        setupDatabase();

        getServer().getPluginManager().registerEvents(this, this);

        new CommandRegistrar(this.getLifecycleManager());
        getLogger().info("Ecomonic Plugin has been enabled!");
    }

    public void setupDatabase() {
        try {
            DatabaseUtils.downloadLibrary(Configurations.datasourceType);
            DatabaseUtils.loadLibrary(Configurations.datasourceType);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        source = switch (Configurations.datasourceType) {
            case SQLITE -> new SQLiteDataSource();
            default -> throw new UnsupportedOperationException("Unsupported value: " + Configurations.datasourceType);
        };

        try {
            source.connect(
                    Configurations.datasourceHost,
                    Map.of(
                            "username", Configurations.datasourceUsername,
                            "password", Configurations.datasourcePassword,
                            "database", Configurations.datasourceDatabase,
                            "port", Configurations.datasourcePort
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        plugin = null;
        if(source!=null) {
            try {
                source.close();
                DatabaseUtils.unloadLibrary();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        getLogger().info("Ecomonic Plugin has been disabled!");
    }

    /**
     * Ecomonic Impl
     * */

    @Override
    public String getVersion() {
        return "0.0.1-SNAPSHOT";
    }

    @Override
    public DataSource getDataSource() {
        return source;
    }

    @Override
    public Account getAccount(long id) {
        if(cachedAccounts == null) return source.getAccount(id, SyncedAccount.class);
        if(cachedAccounts.containsKey(id)) return cachedAccounts.get(id);
        Account a = source.getAccount(id, CachedAccount.class);
        if (a == null) return null;
        cachedAccounts.put(id, a);
        return a;
    }

    @Override
    public Account getPrimaryAccount(UUID uuid) {
        if (primaryAccounts == null) return source.getPrimaryAccount(uuid, SyncedAccount.class);
        if (primaryAccounts.containsKey(uuid)) return primaryAccounts.get(uuid);

        Account a = source.getPrimaryAccount(uuid, CachedAccount.class);
        if (a == null) return null;
        primaryAccounts.put(uuid, a);
        return a;
    }

    @Override
    public void setPrimaryAccount(UUID uuid, Account account) {
        source.setPrimaryAccount(uuid, account.getId());
        if (primaryAccounts == null) return;

        primaryAccounts.put(uuid, account);
    }

    @Override
    public List<Account> getAccounts(UUID uuid) {
        return source.getAccounts(uuid).stream()
                .map(this::getAccount)
                .toList();
    }

    @Override
    public Account createAccount(UUID uuid) {
        long id = TSID.fast().toLong();
        return source.createAccount(id, uuid, cachedAccounts!= null ? CachedAccount.class : SyncedAccount.class);
    }

    @Override
    public Account deleteAccount(long l) {
        source.deleteAccount(l);
        if (cachedAccounts != null && cachedAccounts.containsKey(l)) return cachedAccounts.remove(l);
        return null;
    }

    @Override
    public boolean withdraw(long l, double v) {
        if (cachedAccounts != null && cachedAccounts.containsKey(l)) {
            CachedAccount account = (CachedAccount) cachedAccounts.get(l);
            return account.withdraw(v);
        }else if (source.getAccount(l, SyncedAccount.class) != null) return source.withdraw(l, v);
        return false;
    }

    @Override
    public boolean deposit(long l, double v) {
        if (cachedAccounts != null && cachedAccounts.containsKey(l)) {
            CachedAccount account = (CachedAccount) cachedAccounts.get(l);
            return account.deposit(v);
        }else if (source.getAccount(l, SyncedAccount.class) != null) return source.deposit(l, v);
        return false;
    }

    @Override
    public boolean set(long l, double v) {
        if (cachedAccounts != null && cachedAccounts.containsKey(l)) {
            CachedAccount account = (CachedAccount) cachedAccounts.get(l);
            return account.set(v);
        } else if (source.getAccount(l, SyncedAccount.class) != null) return source.set(l, v);
        return false;
    }

    @Override
    public boolean has(long l, double v) {
        if (cachedAccounts != null && cachedAccounts.containsKey(l)) {
            CachedAccount account = (CachedAccount) cachedAccounts.get(l);
            return account.has(v);
        } else if (source.getAccount(l, SyncedAccount.class) != null) return source.has(l, v);
        return false;
    }

    @Override
    public double getBalance(long id) {
        if (cachedAccounts != null && cachedAccounts.containsKey(id)) {
            CachedAccount account = (CachedAccount) cachedAccounts.get(id);
            return account.getBalance();
        } else if (source.getAccount(id, SyncedAccount.class) != null) return source.getBalance(id);
        throw new IllegalArgumentException("Account not found!");
    }

    @Override
    public void addEventListener(EcomonicListener ecomonicListener) {

    }

    @Override
    public void removeEventListener(EcomonicListener ecomonicListener) {

    }

    @Override
    public void addAccountListener(long l, AccountListener accountListener) {

    }

    @Override
    public void removeAccountListener(long l, AccountListener accountListener) {

    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if(!Configurations.autoCreateAccount) return;
        if(event.getPlayer().getLastSeen() < loadedTime) {
            Account a = Ecomonic.INSTANCE.getPrimaryAccount(event.getPlayer().getUniqueId());
            if(a != null) return;

            a = Ecomonic.INSTANCE.createAccount(event.getPlayer().getUniqueId());
            Ecomonic.INSTANCE.setPrimaryAccount(event.getPlayer().getUniqueId(), a);
        }
    }
}
