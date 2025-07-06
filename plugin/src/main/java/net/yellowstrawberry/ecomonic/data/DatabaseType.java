package net.yellowstrawberry.ecomonic.data;

public enum DatabaseType {
    SQLITE("sqlite", "org.sqlite.JDBC"),
    MYSQL("mysql", ""),
    MARIADB("mariadb", ""),
    POSTGRESQL("postgresql", "org.postgresql.Driver");

    private final String name;
    private final String driver;

    DatabaseType(String name, String driver) {
        this.name = name;
        this.driver = driver;
    }

    public String getName() {
        return name;
    }

    public String getDriver() {
        return driver;
    }

    public static DatabaseType fromName(String name) {
        for (DatabaseType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}