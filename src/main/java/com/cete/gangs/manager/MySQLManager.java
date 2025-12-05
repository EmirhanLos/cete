package com.emirhan.gangs.manager;

import com.emirhan.gangs.GangsPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLManager {

    private final GangsPlugin plugin;
    private Connection connection;

    private String host, database, username, password;
    private int port;
    private boolean ssl;

    public MySQLManager(GangsPlugin plugin) {
        this.plugin = plugin;
        loadSettings();
    }

    public void loadSettings() {
        this.host = plugin.getConfig().getString("storage.mysql.host");
        this.port = plugin.getConfig().getInt("storage.mysql.port");
        this.database = plugin.getConfig().getString("storage.mysql.database");
        this.username = plugin.getConfig().getString("storage.mysql.username");
        this.password = plugin.getConfig().getString("storage.mysql.password");
        this.ssl = plugin.getConfig().getBoolean("storage.mysql.ssl");
    }

    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) return;

            synchronized (this) {
                if (connection != null && !connection.isClosed()) return;
                // JDBC sürücüsü
                // Not: Yeni MySQL sürücüleri için "com.mysql.cj.jdbc.Driver" kullanılabilir, 
                // eski versiyonlar veya Spigot dahili sürücüsü için "com.mysql.jdbc.Driver" yaygındır.
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    Class.forName("com.mysql.jdbc.Driver");
                }
                
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true", username, password);
                plugin.getLogger().info("MySQL bağlantısı başarılı!");
                setupTables();
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().severe("MySQL bağlantısı kurulamadı: " + e.getMessage());
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    private void setupTables() {
        try (Statement statement = getConnection().createStatement()) {
            // Çeteler tablosu
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS cete_gangs (" +
                    "id VARCHAR(32) PRIMARY KEY, " +
                    "name VARCHAR(64), " +
                    "leader VARCHAR(36), " +
                    "friendlyFire BOOLEAN, " +
                    "bank DOUBLE, " +
                    "kills INT, " +
                    "deaths INT)");

            // Üyeler ve rütbeleri
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS cete_members (" +
                    "uuid VARCHAR(36), " +
                    "gang_id VARCHAR(32), " +
                    "rank VARCHAR(16), " +
                    "PRIMARY KEY (uuid))");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}