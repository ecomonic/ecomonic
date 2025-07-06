package net.yellowstrawberry.ecomonic.data;

public enum DatabaseType {
    SQLITE("sqlite"),
    MYSQL("mysql"),
    MARIADB("mariadb"),
    POSTGRESQL("postgresql");

    private final String name;

    DatabaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DatabaseType fromName(String name) {
        for (DatabaseType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) return type;
        }
        return null;
    }
}