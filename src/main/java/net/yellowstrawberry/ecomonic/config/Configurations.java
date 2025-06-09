package net.yellowstrawberry.ecomonic.config;

import net.yellowstrawberry.ecomonic.EcomonicPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class Configurations {
    public static final File root = EcomonicPlugin.plugin.getDataFolder();
    public static File database;
    public static boolean cache = true;
    public static String datasourceType = "sqlite";
    public static String datasourceHost = "./economic.db";
    public static List<String> loggingStrategy;
    public static String logFile = "./economic.log";

    public Configurations() {
        if (!root.exists()) root.mkdirs();
        File configFile = new File(root, "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = EcomonicPlugin.class.getClassLoader().getResourceAsStream("template/config.yml")) {
                if (in == null) throw new IllegalStateException("Failed to load config.yml from resources.");
                java.nio.file.Files.copy(in, configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                EcomonicPlugin.plugin.getLogger().info("Default config.yml copied to plugin folder.");
            } catch (Exception e) {
                EcomonicPlugin.plugin.getLogger().severe("Failed to copy default config.yml: " + e.getMessage());
            }
        }
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(fis);
            if (config.containsKey("logging")) {
                Map<String, Object> logging = (Map<String, Object>) config.get("logging");
                if (logging.get("strategy") instanceof List) {
                    loggingStrategy = (List<String>) logging.get("strategy");
                } else if (logging.get("strategy") instanceof String) {
                    loggingStrategy = List.of((String) logging.get("strategy"));
                }
                if (logging.containsKey("file")) {
                    logFile = (String) logging.get("file");
                }
            }
            if (config.containsKey("datasource")) {
                Map<String, Object> datasource = (Map<String, Object>) config.get("datasource");
                if (datasource.containsKey("type")) {
                    datasourceType = (String) datasource.get("type");
                }
                if (datasource.containsKey("host")) {
                    datasourceHost = (String) datasource.get("host");
                }
                if (datasource.containsKey("cache")) {
                    String cacheVal = ((String) datasource.get("cache")).toLowerCase();
                    cache = cacheVal.equals("yes");
                }
            }
            // Set database file for sqlite
            if (datasourceType.equalsIgnoreCase("sqlite")) {
                database = new File(datasourceHost);
                if (!database.isAbsolute()) {
                    database = root.toPath().resolve(datasourceHost).toFile();
                }
                if (!database.exists()) {
                    database.createNewFile();
                }
            }
        } catch (Exception e) {
            EcomonicPlugin.plugin.getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }
    }
}
