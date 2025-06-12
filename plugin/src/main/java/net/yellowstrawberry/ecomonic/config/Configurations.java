package net.yellowstrawberry.ecomonic.config;

import net.yellowstrawberry.ecomonic.EcomonicPlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class Configurations {
    public static final File root = EcomonicPlugin.plugin.getDataFolder();
    public static File database;
    public static boolean cache = true;
    public static String datasourceType = "sqlite";
    public static String datasourceHost = "./ecomonic.db";
    public static List<String> loggingStrategy;
    public static String logFile = root+"./ecomonic.log";

    // Account and transaction settings
    public static boolean autoCreateAccount = true;
    public static int maxAccounts = 3;
    public static boolean enableNegativeBalance = false;
    public static double maxNegativeBalance = 0.0;

    public static boolean enableTransactionFees = false;
    public static double transactionFeePercentage = 0.0;
    public static double maxTransactionAmount = Double.MAX_VALUE;
    public static double minTransactionAmount = 0.0;
    public static int transactionCooldown = 0;

    public static void load() {
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
                    Files.copy(EcomonicPlugin.class.getClassLoader().getResourceAsStream("template/ecomonic.db"), database.toPath());
                }
            }
            if (config.containsKey("account")) {
                Map<String, Object> account = (Map<String, Object>) config.get("account");
                if (account.containsKey("auto_create")) {
                    autoCreateAccount = Boolean.parseBoolean(account.get("auto_create").toString());
                }
                if (account.containsKey("max_accounts")) {
                    maxAccounts = Integer.parseInt(account.get("max_accounts").toString());
                }
                if (account.containsKey("enable_negative_balance")) {
                    enableNegativeBalance = Boolean.parseBoolean(account.get("enable_negative_balance").toString());
                }
                if (account.containsKey("max_negative_balance")) {
                    maxNegativeBalance = Double.parseDouble(account.get("max_negative_balance").toString());
                }
            }
            if (config.containsKey("transaction")) {
                Map<String, Object> transaction = (Map<String, Object>) config.get("transaction");
                if (transaction.containsKey("enable_transaction_fees")) {
                    enableTransactionFees = Boolean.parseBoolean(transaction.get("enable_transaction_fees").toString());
                }
                if (transaction.containsKey("transaction_fee_percentage")) {
                    transactionFeePercentage = Double.parseDouble(transaction.get("transaction_fee_percentage").toString());
                }
                if (transaction.containsKey("max_transaction_amount")) {
                    maxTransactionAmount = Double.parseDouble(transaction.get("max_transaction_amount").toString());
                }
                if (transaction.containsKey("min_transaction_amount")) {
                    minTransactionAmount = Double.parseDouble(transaction.get("min_transaction_amount").toString());
                }
                if (transaction.containsKey("transaction_cooldown")) {
                    transactionCooldown = Integer.parseInt(transaction.get("transaction_cooldown").toString());
                }
            }
        } catch (Exception e) {
            EcomonicPlugin.plugin.getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }
    }
}
