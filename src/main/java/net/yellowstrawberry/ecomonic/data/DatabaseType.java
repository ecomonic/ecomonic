package net.yellowstrawberry.ecomonic.data;

public enum DatabaseType {
    SQLITE("sqlite"),
    MYSQL("mysql"),
    MariaDB("mariadb"),
    POSTGRESQL("postgresql");

    private final String name;

    DatabaseType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}