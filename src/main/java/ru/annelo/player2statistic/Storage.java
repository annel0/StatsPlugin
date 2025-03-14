package ru.annelo.player2statistic;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

// Интерфейс для работы с хранилищем
interface IStorage {
    void savePlayerStats(PlayerStats stats);

    PlayerStats loadPlayerStats(UUID uuid);

    void loadAllPlayers();

    void saveAllPlayers();
}


// Реализация локального хранилища (YAML)
class FileStorage implements IStorage {
    private final StatsPlugin plugin;

    public FileStorage(StatsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void savePlayerStats(PlayerStats stats) {
        try {
            // Получаем путь к файлу
            File dataFolder = plugin.getDataFolder();
            File statsFile = new File(dataFolder, "stats/" + stats.getUuid().toString() + ".yml");

            // Если директория не существует, создаем её
            if (!dataFolder.exists()) {
                if (!dataFolder.mkdirs()) {
                    Bukkit.getLogger()
                            .severe("Не удалось создать директорию для сохранения статистики!");
                }
            }

            // Создаем конфигурацию
            YamlConfiguration config = new YamlConfiguration();
            config.set("uuid", stats.getUuid().toString());
            config.set("playTime", stats.getPlayTime());
            config.set("mobsKilled", stats.getMobsKilled());
            config.set("itemsEaten", stats.getItemsEaten());
            config.set("distanceTraveled", stats.getDistanceTraveled());
            config.set("blocksBroken", stats.getBlocksBroken());
            config.set("deaths", stats.getDeaths());
            config.set("itemsCrafted", stats.getItemsCrafted());
            config.set("itemsUsed", stats.getItemsUsed());
            config.set("chestsOpened", stats.getChestsOpened());
            config.set("messagesSent", stats.getMessagesSent());

            // Сохраняем в файл
            config.save(statsFile);
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error saving player stats for " + stats.getUuid());
            e.printStackTrace();
        }
    }

    @Override
    public PlayerStats loadPlayerStats(UUID uuid) {
        File dataFolder = plugin.getDataFolder();
        File statsFile = new File(dataFolder, "stats/" + uuid.toString() + ".yml");

        if (!statsFile.exists()) {
            return null;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);

            PlayerStats stats = new PlayerStats();
            stats.setUuid(UUID.fromString(config.getString("uuid")));
            stats.setPlayTime(config.getInt("playTime"));
            stats.setMobsKilled(config.getInt("mobsKilled"));
            stats.setItemsEaten(config.getInt("itemsEaten"));
            stats.setDistanceTraveled(config.getDouble("distanceTraveled"));
            stats.setBlocksBroken(config.getInt("blocksBroken"));
            stats.setDeaths(config.getInt("deaths"));
            stats.setItemsCrafted(config.getInt("itemsCrafted"));
            stats.setItemsUsed(config.getInt("itemsUsed"));
            stats.setChestsOpened(config.getInt("chestsOpened"));
            stats.setMessagesSent(config.getInt("messagesSent"));

            return stats;
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error loading player stats for " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void loadAllPlayers() {
        File dataFolder = plugin.getDataFolder();
        File statsDir = new File(dataFolder, "stats");

        if (!statsDir.exists()) {
            statsDir.mkdir();
            return;
        }

        for (File file : statsDir.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                PlayerStats stats = loadPlayerStats(uuid);
                if (stats != null) {
                    Bukkit.getLogger().info("Loaded stats for " + uuid);
                }
            }
        }
    }

    @Override
    public void saveAllPlayers() {
        File dataFolder = plugin.getDataFolder();
        File statsDir = new File(dataFolder, "stats");

        if (!statsDir.exists()) {
            statsDir.mkdir();
        }

        for (File file : statsDir.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                PlayerStats stats = loadPlayerStats(uuid);
                if (stats != null) {
                    savePlayerStats(stats);
                }
            }
        }
    }

}


class DatabaseStorage implements IStorage {
    private Connection connection;

    public DatabaseStorage(String host, int port, String database, String user, String password) {
        try {
            // Создаем соединение
            Class.forName("org.mariadb.jdbc.Driver");
            String url = "jdbc:mariadb://" + host + ":" + port + "/" + database;
            connection = DriverManager.getConnection(url, user, password);

            // Создаем таблицу, если её нет
            PreparedStatement stmt =
                    connection.prepareStatement("CREATE TABLE IF NOT EXISTS player_stats ("
                            + "uuid CHAR(36) PRIMARY KEY," + "play_time INT," + "mobs_killed INT,"
                            + "items_eaten INT," + "distance_traveled DOUBLE,"
                            + "blocks_broken INT," + "deaths INT," + "items_crafted INT,"
                            + "items_used INT," + "chests_opened INT," + "messages_sent INT" + ")");
            stmt.execute();
            stmt.close();
        } catch (SQLException | ClassNotFoundException e) {
            Bukkit.getLogger().severe("Error connecting to database");
            e.printStackTrace();
        }
    }

    @Override
    public void savePlayerStats(PlayerStats stats) {
        CompletableFuture.runAsync(() -> {
            try {
                PreparedStatement stmt = connection.prepareStatement(
                        "INSERT INTO player_stats (uuid, play_time, mobs_killed, items_eaten, distance_traveled, blocks_broken, deaths, items_crafted, items_used, chests_opened, messages_sent) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                + "ON DUPLICATE KEY UPDATE " + "play_time = VALUES(play_time),"
                                + "mobs_killed = VALUES(mobs_killed),"
                                + "items_eaten = VALUES(items_eaten),"
                                + "distance_traveled = VALUES(distance_traveled),"
                                + "blocks_broken = VALUES(blocks_broken),"
                                + "deaths = VALUES(deaths),"
                                + "items_crafted = VALUES(items_crafted),"
                                + "items_used = VALUES(items_used),"
                                + "chests_opened = VALUES(chests_opened),"
                                + "messages_sent = VALUES(messages_sent)");

                stmt.setString(1, stats.getUuid().toString());
                stmt.setInt(2, stats.getPlayTime());
                stmt.setInt(3, stats.getMobsKilled());
                stmt.setInt(4, stats.getItemsEaten());
                stmt.setDouble(5, stats.getDistanceTraveled());
                stmt.setInt(6, stats.getBlocksBroken());
                stmt.setInt(7, stats.getDeaths());
                stmt.setInt(8, stats.getItemsCrafted());
                stmt.setInt(9, stats.getItemsUsed());
                stmt.setInt(10, stats.getChestsOpened());
                stmt.setInt(11, stats.getMessagesSent());

                stmt.executeUpdate();
                stmt.close();
            } catch (SQLException e) {
                Bukkit.getLogger().severe("Error saving player stats for " + stats.getUuid());
                e.printStackTrace();
            }
        });
    }

    @Override
    public PlayerStats loadPlayerStats(UUID uuid) {
        try {
            PreparedStatement stmt =
                    connection.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?");
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                PlayerStats stats = new PlayerStats();
                stats.setUuid(uuid);
                stats.setPlayTime(rs.getInt("play_time"));
                stats.setMobsKilled(rs.getInt("mobs_killed"));
                stats.setItemsEaten(rs.getInt("items_eaten"));
                stats.setDistanceTraveled(rs.getDouble("distance_traveled"));
                stats.setBlocksBroken(rs.getInt("blocks_broken"));
                stats.setDeaths(rs.getInt("deaths"));
                stats.setItemsCrafted(rs.getInt("items_crafted"));
                stats.setItemsUsed(rs.getInt("items_used"));
                stats.setChestsOpened(rs.getInt("chests_opened"));
                stats.setMessagesSent(rs.getInt("messages_sent"));

                rs.close();
                stmt.close();
                return stats;
            }

            rs.close();
            stmt.close();
            return null;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error loading player stats for " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void loadAllPlayers() {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_stats");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                PlayerStats stats = new PlayerStats();
                stats.setUuid(uuid);
                stats.setPlayTime(rs.getInt("play_time"));
                stats.setMobsKilled(rs.getInt("mobs_killed"));
                stats.setItemsEaten(rs.getInt("items_eaten"));
                stats.setDistanceTraveled(rs.getDouble("distance_traveled"));
                stats.setBlocksBroken(rs.getInt("blocks_broken"));
                stats.setDeaths(rs.getInt("deaths"));
                stats.setItemsCrafted(rs.getInt("items_crafted"));
                stats.setItemsUsed(rs.getInt("items_used"));
                stats.setChestsOpened(rs.getInt("chests_opened"));
                stats.setMessagesSent(rs.getInt("messages_sent"));
                Bukkit.getLogger().info("Loaded stats for " + uuid);
            }

            rs.close();
            stmt.close();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error loading all player stats");
            e.printStackTrace();
        }
    }

    @Override
    public void saveAllPlayers() {
        // Для базы данных сохранение всех игроков происходит автоматически при каждом
        // savePlayerStats()
    }
}
