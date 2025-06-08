package net.yellowstrawberry.ecomonic;

import net.yellowstrawberry.ecomonic.api.Ecomonic;
import net.yellowstrawberry.ecomonic.command.CommandRegistrar;
import org.bukkit.plugin.java.JavaPlugin;

public class EcomonicPlugin extends JavaPlugin {

    public static Ecomonic ecomonic;
    public static EcomonicPlugin plugin;

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onEnable() {
        plugin = this;
        getLogger().info("Ecomonic Plugin has been enabled!");
        new CommandRegistrar(this.getLifecycleManager());
    }

    @Override
    public void onDisable() {
        getLogger().info("Ecomonic Plugin has been disabled!");
        plugin = null;
    }

    public static Ecomonic getEcomonic() {
        return ecomonic;
    }
}
