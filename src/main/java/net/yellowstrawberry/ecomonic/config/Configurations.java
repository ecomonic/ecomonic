package net.yellowstrawberry.ecomonic.config;

import net.yellowstrawberry.ecomonic.EcomonicPlugin;

import java.io.File;

public class Configurations {

    public static final File root = EcomonicPlugin.plugin.getDataFolder();
    public static final File database = root.toPath().resolve("ecomonic.db").toFile();

    public Configurations() {
        if (!root.exists()) root.mkdirs();
        if (!database.exists()) {
            try {
                database.createNewFile();
            } catch (Exception e) {
                EcomonicPlugin.plugin.getLogger().severe("Failed to create database file: " + e.getMessage());
            }
        }
    }
}
