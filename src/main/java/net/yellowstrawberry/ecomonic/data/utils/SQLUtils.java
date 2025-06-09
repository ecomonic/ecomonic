package net.yellowstrawberry.ecomonic.data.utils;


import net.yellowstrawberry.ecomonic.EcomonicPlugin;

import java.sql.*;

/**
 * SQLUtils is a utility class for managing SQL database connections and executing SQL statements.
 * It provides methods to execute SQL commands, queries, and updates with support for parameterized statements.
 * The class handles connection management, including automatic reconnection if the connection is lost.
 * @author yellowstrawberrys
 * @version 0.0.1
 * @since 0.0.1
 */
public class SQLUtils {

    private Connection connection;
    private final String url;
    private final String user;
    private final String pass;
    public SQLUtils(String url) {
        this.url = url;
        this.user = null;
        this.pass = null;
        connect();
    }

    public SQLUtils(String type, String host, String user, String pass, String database) {
        this.url = "jdbc:"+type+"://"+host+"/"+database+"?connectTimeout=0&socketTimeout=0&autoReconnect=true";
        this.user = user;
        this.pass = pass;
        connect();
    }

    private boolean isReachable() throws SQLException {
        return !getConnection().isClosed()&&getConnection().isValid(1000);
    }

    /**
     * SQLCommunicator#executeN(String, Object...)
     * @param statement an SQL statement that may contain one or more '?' IN parameter placeholders
     * @param values values that can replace ? IN statement
     * */
    public void execute(String statement, Object... values) {
        try (PreparedStatement stmt = getConnection().prepareStatement(statement)){
            for(int i = 0; i< values.length; i++) {
                stmt.setObject(1+i, values[i]);
            }
            stmt.execute();
        } catch (SQLException e) {
            try {
                if(isReachable()){
                    throw new RuntimeException(e);
                }else {
                    connect();
                    execute(statement, values);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * SQLCommunicator#executeQueryN(String, Object...)
     * @param statement an SQL statement that may contain one or more '?' IN parameter placeholders
     * @param values values that can replace ? IN statement
     * @return a ResultSet object that contains the data produced by the query; never null
     * @throws SQLException if a database access error occurs; this method is called on a closed PreparedStatement or the SQL statement does not return a ResultSet object
     * */
    public ResultSet executeQuery(String statement, Object... values) throws SQLException {
        try {
            PreparedStatement stmt = getConnection().prepareStatement(statement);
            for(int i = 0; i< values.length; i++) {
                stmt.setObject(1+i, values[i]);
            }
            return stmt.executeQuery();
        }catch (SQLException e) {
            if(isReachable()){
                throw e;
            }else {
                connect();
                return executeQuery(statement, values);
            }
        }
    }

    /**
     * SQLCommunicator#executeUpdateN(String, Object...)
     * @param statement an SQL statement that may contain one or more '?' IN parameter placeholders
     * @param values values that can replace ? IN statement
     * @return either (1) the row count for SQL Data Manipulation Language (DML) statements or (2) 0 for SQL statements that return nothing
     * */
    public int executeUpdate(String statement, Object... values) {
        try (PreparedStatement stmt = getConnection().prepareStatement(statement)){
            for(int i = 0; i< values.length; i++) {
                stmt.setObject(1+i, values[i]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            try {
                if(isReachable()){
                    throw new RuntimeException(e);
                }else {
                    connect();
                    return executeUpdate(statement, values);
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void connect() {
        try {
            EcomonicPlugin.plugin.getLogger().info("Making connection with the database...");
            Class.forName("org.mariadb.jdbc.Driver");
            connection = user != null && pass != null ? DriverManager.getConnection(
                    url,
                    user,
                    pass
            ) : DriverManager.getConnection(url);
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        } finally {
            EcomonicPlugin.plugin.getLogger().info("Success to make connection with the database!");
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close the SQL connection", e);
        }
    }
}