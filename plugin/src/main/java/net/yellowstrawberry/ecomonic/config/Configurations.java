package net.yellowstrawberry.ecomonic.config;

import net.yellowstrawberry.ecomonic.EcomonicPlugin;
import net.yellowstrawberry.ecomonic.data.DatabaseType;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class Configurations {
    public static final File root = EcomonicPlugin.plugin.getDataFolder();
    public static DatabaseType datasourceType = DatabaseType.SQLITE;
    public static String datasourceHost = "./ecomonic.db";
    public static String datasourceDatabase = "ecomonic";
    public static String datasourceUsername = "ecomonic";
    public static String datasourcePassword = "Ch@ngeTh!s3cr3t";
    public static String datasourcePort = "3306";
    public static List<String> loggingStrategy;
    public static String logFile = root+"./ecomonic.log";

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
        createDefaultConfig(configFile);
        
        try (FileInputStream fis = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(fis);

            loadLoggingConfig(config);
            loadDatasourceConfig(config);
            loadAccountConfig(config);
            loadTransactionConfig(config);
            
        } catch (Exception e) {
            EcomonicPlugin.plugin.getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }
    }

    private static void createDefaultConfig(File configFile) {
        if (configFile.exists()) return;

        try (InputStream in = EcomonicPlugin.class.getClassLoader().getResourceAsStream("template/config.yml")) {
            if (in == null) throw new IllegalStateException("Failed to load config.yml from resources.");
            Files.copy(in, configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            EcomonicPlugin.plugin.getLogger().info("Default config.yml copied to plugin folder.");
        } catch (Exception e) {
            EcomonicPlugin.plugin.getLogger().severe("Failed to copy default config.yml: " + e.getMessage());
        }
    }

    private static void loadLoggingConfig(Map<String, Object> config) {
        if (!config.containsKey("logging")) return;
        
        Map<String, Object> logging = (Map<String, Object>) config.get("logging");
        if (logging.get("strategy") instanceof List) loggingStrategy = (List<String>) logging.get("strategy");
        else if (logging.get("strategy") instanceof String) loggingStrategy = List.of((String) logging.get("strategy"));

        if (logging.containsKey("file")) logFile = (String) logging.get("file");
    }

    private static void loadDatasourceConfig(Map<String, Object> config) {
        if (!config.containsKey("datasource")) return;

        Map<String, Object> datasource = (Map<String, Object>) config.get("datasource");
        if (datasource.containsKey("type")) datasourceType = DatabaseType.fromName((String) datasource.get("type"));
        if (datasource.containsKey("host")) datasourceHost = (String) datasource.get("host");
        if (datasource.containsKey("database")) datasourceDatabase = (String) datasource.get("database");
        if (datasource.containsKey("username")) datasourceUsername = (String) datasource.get("username");
        if (datasource.containsKey("password")) datasourcePassword = (String) datasource.get("password");
        if (datasource.containsKey("port")) datasourcePort = String.valueOf(datasource.get("port"));

        if (datasourceType == DatabaseType.SQLITE) setupSqliteDatabase();
    }

    private static void setupSqliteDatabase() {
        File database = new File(datasourceHost);
        if (!database.isAbsolute()) database = root.toPath().resolve(datasourceHost).toFile();

        if (!database.exists()) {
            try {
                Files.copy(EcomonicPlugin.class.getClassLoader().getResourceAsStream("template/ecomonic.db"), database.toPath());
            } catch (Exception e) {
                EcomonicPlugin.plugin.getLogger().severe("Failed to create SQLite database: " + e.getMessage());
            }
        }
    }

    private static void loadAccountConfig(Map<String, Object> config) {
        if (!config.containsKey("account")) return;

        Map<String, Object> account = (Map<String, Object>) config.get("account");
        if (account.containsKey("auto_create")) autoCreateAccount = Boolean.parseBoolean(account.get("auto_create").toString());
        if (account.containsKey("max_accounts")) maxAccounts = Integer.parseInt(account.get("max_accounts").toString());
        if (account.containsKey("enable_negative_balance")) enableNegativeBalance = Boolean.parseBoolean(account.get("enable_negative_balance").toString());
        if (account.containsKey("max_negative_balance")) maxNegativeBalance = Double.parseDouble(account.get("max_negative_balance").toString());
    }

    private static void loadTransactionConfig(Map<String, Object> config) {
        if (!config.containsKey("transaction")) return;

        Map<String, Object> transaction = (Map<String, Object>) config.get("transaction");
        if (transaction.containsKey("enable_transaction_fees")) enableTransactionFees = Boolean.parseBoolean(transaction.get("enable_transaction_fees").toString());
        if (transaction.containsKey("transaction_fee_percentage")) transactionFeePercentage = Double.parseDouble(transaction.get("transaction_fee_percentage").toString());
        if (transaction.containsKey("max_transaction_amount")) maxTransactionAmount = Double.parseDouble(transaction.get("max_transaction_amount").toString());
        if (transaction.containsKey("min_transaction_amount")) minTransactionAmount = Double.parseDouble(transaction.get("min_transaction_amount").toString());
        if (transaction.containsKey("transaction_cooldown")) transactionCooldown = Integer.parseInt(transaction.get("transaction_cooldown").toString());
    }
}
