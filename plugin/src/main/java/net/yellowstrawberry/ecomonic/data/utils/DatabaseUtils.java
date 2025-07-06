package net.yellowstrawberry.ecomonic.data.utils;

import net.yellowstrawberry.ecomonic.config.Configurations;
import net.yellowstrawberry.ecomonic.data.DatabaseType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseUtils {

    private static URLClassLoader DATABASE_CLASS_LOADER = null;
    private static DriverShim driverShim;

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

        ReadableByteChannel readableByteChannel = Channels.newChannel(URI.create(url).toURL().openStream());
        if(!f.getParentFile().exists() && !f.getParentFile().mkdirs()) throw new RuntimeException("Failed to create directory '%s'".formatted(f.getParentFile().getAbsolutePath()));
        try(FileOutputStream fos = new FileOutputStream(f)) {
            FileChannel fc = fos.getChannel();
            fc.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        }
    }

    public static void loadLibrary(DatabaseType type) throws MalformedURLException {
        File f = new File(Configurations.root + "/lib/" + type + ".jar");
        DATABASE_CLASS_LOADER = new URLClassLoader(new URL[]{f.toURI().toURL()}, DatabaseUtils.class.getClassLoader());

        try {
            Class<?> driver = Class.forName(type.getDriver(), true, DATABASE_CLASS_LOADER);
            driverShim = new DriverShim((Driver) driver.getDeclaredConstructor().newInstance());
            DriverManager.registerDriver(driverShim);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException | SQLException e) {
            throw new RuntimeException("Failed to register driver", e);
        }
    }

    public static void unloadLibrary() throws IOException, SQLException {
        DriverManager.deregisterDriver(driverShim);
        driverShim = null;
        DATABASE_CLASS_LOADER.close();
    }

    public static class DriverShim implements Driver {
        private final Driver driver;

        public DriverShim(Driver d) {
            this.driver = d;
        }

        public boolean acceptsURL(String u) throws SQLException {
            return this.driver.acceptsURL(u);
        }

        public Connection connect(String u, Properties p) throws SQLException {
            return this.driver.connect(u, p);
        }

        public int getMajorVersion() {
            return this.driver.getMajorVersion();
        }

        public int getMinorVersion() {
            return this.driver.getMinorVersion();
        }

        public DriverPropertyInfo[] getPropertyInfo(String u, Properties p) throws SQLException {
            return this.driver.getPropertyInfo(u, p);
        }

        public boolean jdbcCompliant() {
            return this.driver.jdbcCompliant();
        }

        public Logger getParentLogger() {
            throw new UnsupportedOperationException();
        }
    }
}
