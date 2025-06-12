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
import net.yellowstrawberry.ecomonic.data.sqlite.SQLiteDataSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class EcomonicPlugin extends JavaPlugin implements Ecomonic, Listener {

    public static EcomonicPlugin plugin;
    private static DataSource source = new SQLiteDataSource();
    private HashMap<UUID, Account> primaryAccounts = new HashMap<>();
    private Long2ObjectMap<Account> cachedAccounts = new Long2ObjectOpenHashMap<>();

    private long loadedTime;

    /**
     * Plugin
     * */

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onEnable() {
        plugin = this;

        Configurations.load();

        try {
            source.connect(Configurations.database.toString(), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        getServer().getPluginManager().registerEvents(this, this);

        loadedTime = System.currentTimeMillis();
        new CommandRegistrar(this.getLifecycleManager());
        getLogger().info("Ecomonic Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        try {
            source.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        plugin = null;
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
