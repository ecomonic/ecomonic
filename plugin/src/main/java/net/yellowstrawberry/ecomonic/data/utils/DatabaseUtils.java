package net.yellowstrawberry.ecomonic.data.utils;

import net.yellowstrawberry.ecomonic.config.Configurations;
import net.yellowstrawberry.ecomonic.data.DatabaseType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public class DatabaseUtils {

    public static URLClassLoader DATABASE_CLASS_LOADER = null;

    public static void downloadLibrary(DatabaseType type) throws IOException {
        File f = new File(Configurations.root + "/lib/" + type + ".jar");
        if(f.exists()) return;

        String url;
        switch (type) {
            case MYSQL -> url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.3.0/mysql-connector-j-9.3.0.jar";
            case MARIADB -> url = "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.4/mariadb-java-client-3.5.4.jar";
            case POSTGRESQL -> url = "https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.7/postgresql-42.7.7.jar";
            case SQLITE -> url = "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.50.2.0/sqlite-jdbc-3.50.2.0.jar";
            default -> url = null;
        }

        Files.copy(Path.of(url), f.toPath());
    }

    public static void loadLibrary(DatabaseType type) throws MalformedURLException {
        File f = new File(Configurations.root + "/lib/" + type + ".jar");
        DATABASE_CLASS_LOADER = new URLClassLoader(new URL[]{f.toURI().toURL()}, DatabaseUtils.class.getClassLoader());
    }

    public static void unloadLibrary() throws IOException {
        DATABASE_CLASS_LOADER.close();
    }
}
