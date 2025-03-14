package ru.annelo.player2statistic;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

// Интерфейс для работы с хранилищем
interface IStorage {
    void savePlayerStats(PlayerStats stats);

    PlayerStats loadPlayerStats(UUID uuid);

    List<PlayerStats> loadAllPlayers();

    void saveAllPlayers();

    void reloadStorage();

    void close();

    List<PlayerStats> getTopStats(StatType playTime, int limit);
}


// Реализация локального хранилища (YAML)
class FileStorage implements IStorage {
    private final JavaPlugin plugin;

    public FileStorage(JavaPlugin plugin2) {
        this.plugin = plugin2;
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
            config.set("player_name", stats.getPlayerName());
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
            // Файл не существует, создаём новый файл, сохраняем его и возвращаем пустые статистики
            PlayerStats newStats = new PlayerStats();
            newStats.setUuid(uuid);
            savePlayerStats(newStats);
            return newStats;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);

            PlayerStats stats = new PlayerStats();
            stats.setUuid(UUID.fromString(config.getString("uuid")));
            stats.setPlayerName(config.getString("player_name"));
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
    public List<PlayerStats> loadAllPlayers() {
        File dataFolder = plugin.getDataFolder();
        File statsDir = new File(dataFolder, "stats");

        List<PlayerStats> statsList = new ArrayList<>();

        if (!statsDir.exists()) {
            statsDir.mkdir();
            return null;
        }

        for (File file : statsDir.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                PlayerStats stats = loadPlayerStats(uuid);
                if (stats != null) {
                    Bukkit.getLogger().info("Loaded stats for " + uuid);
                    statsList.add(stats);
                }
            }
        }

        return statsList;
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

    @Override
    public void reloadStorage() {
        loadAllPlayers();
    }

    @Override
    public void close() {
        saveAllPlayers();
        Bukkit.getLogger().info("Saved all players' stats to YAML files.");
    }

    @Override
    public List<PlayerStats> getTopStats(StatType statType, int limit) {
        List<PlayerStats> statsList = loadAllPlayers();

        if (statsList == null)
            return null;

        // Фильтруем статистику по типу
        List<PlayerStats> filteredStats = new ArrayList<>();
        for (PlayerStats stats : statsList) {
            switch (statType) {
                case PLAY_TIME:
                    if (stats.getPlayTime() > 0)
                        filteredStats.add(stats);
                    break;
                case MOBS_KILLED:
                    if (stats.getMobsKilled() > 0)
                        filteredStats.add(stats);
                    break;
                case ITEMS_EATEN:
                    if (stats.getItemsEaten() > 0)
                        filteredStats.add(stats);
                case DISTANCE_TRAVELED:
                    if (stats.getDistanceTraveled() > 0)
                        filteredStats.add(stats);
                case BLOCKS_BROKEN:
                    if (stats.getBlocksBroken() > 0)
                        filteredStats.add(stats);
                case CHEST_OPENED:
                    if (stats.getChestsOpened() > 0)
                        filteredStats.add(stats);
            }
        }

        // Сортируем список по выбранному типу статистики
        switch (statType) {
            case PLAY_TIME:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getPlayTime).reversed());
                break;
            case MOBS_KILLED:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getMobsKilled).reversed());
                break;
            case ITEMS_EATEN:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getItemsEaten).reversed());
            case BLOCKS_BROKEN:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getBlocksBroken).reversed());
                break;
            case CHEST_OPENED:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getChestsOpened).reversed());
                break;
            case DISTANCE_TRAVELED:
                Collections.sort(filteredStats,
                        Comparator.comparingDouble(PlayerStats::getDistanceTraveled).reversed());
                break;
            default:
                break;
        }

        if (limit > filteredStats.size()) {
            return filteredStats;
        }
        if (limit <= 0) {
            return filteredStats;
        }

        return filteredStats.subList(0, Math.min(filteredStats.size(), limit));
    }
}


class DatabaseStorage implements IStorage {
    private Connection connection;
    private Config config;

    public DatabaseStorage(Config config) {
        this.config = config;

        try {
            // Создаем соединение
            Class.forName("org.mariadb.jdbc.Driver");
            String url = "jdbc:mariadb://" + config.getDatabaseHost() + ":"
                    + config.getDatabasePort() + "/" + config.getDatabaseName();
            connection = DriverManager.getConnection(url, config.getDatabaseUsername(),
                    config.getDatabasePassword());

            // Создаем таблицу, если её нет
            PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_stats (" + "uuid CHAR(36) PRIMARY KEY,"
                            + "player_name STRING," + "play_time INT," + "mobs_killed INT,"
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
                        "INSERT INTO player_stats (uuid, player_name, play_time, mobs_killed, items_eaten, distance_traveled, blocks_broken, deaths, items_crafted, items_used, chests_opened, messages_sent) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                                + "ON DUPLICATE KEY UPDATE " + "player_name = VALUES(player_name),"
                                + "play_time = VALUES(play_time),"
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
                stmt.setString(2, stats.getPlayerName());
                stmt.setInt(3, stats.getPlayTime());
                stmt.setInt(4, stats.getMobsKilled());
                stmt.setInt(5, stats.getItemsEaten());
                stmt.setDouble(6, stats.getDistanceTraveled());
                stmt.setInt(7, stats.getBlocksBroken());
                stmt.setInt(8, stats.getDeaths());
                stmt.setInt(9, stats.getItemsCrafted());
                stmt.setInt(10, stats.getItemsUsed());
                stmt.setInt(11, stats.getChestsOpened());
                stmt.setInt(12, stats.getMessagesSent());

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
                stats.setPlayerName(rs.getString("player_name"));

                rs.close();
                stmt.close();
                return stats;
            } else {
                rs.close();
                stmt.close();
                // Если нет статистики для игрока, создаём новую? сохроняем и возвращаем её
                PlayerStats newStats = new PlayerStats();
                newStats.setUuid(uuid);
                savePlayerStats(newStats);
                return newStats;
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error loading player stats for " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<PlayerStats> loadAllPlayers() {

        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_stats");
            ResultSet rs = stmt.executeQuery();

            List<PlayerStats> statsList = new ArrayList<>();

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
                stats.setPlayerName(rs.getString("player_name"));

                statsList.add(stats);

                Bukkit.getLogger().info("Loaded stats for " + uuid);
            }

            rs.close();
            stmt.close();
            return statsList;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error loading all player stats");
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveAllPlayers() {
        // Для базы данных сохранение всех игроков происходит автоматически при каждом
        // savePlayerStats()
    }

    @Override
    public void reloadStorage() {
        if (connection == null) {
            try {
                // Закрываем старое соединение, если оно существует
                if (connection != null) {
                    connection.close();
                }

                // Создаем новое соединение
                Class.forName("org.mariadb.jdbc.Driver");
                String url = "jdbc:mariadb://" + config.getDatabaseHost() + ":"
                        + config.getDatabasePort() + "/" + config.getDatabaseName();
                connection = DriverManager.getConnection(url, config.getDatabaseUsername(),
                        config.getDatabasePassword());

                // Проверяем, существует ли таблица player_stats
                PreparedStatement stmt =
                        connection.prepareStatement("CREATE TABLE IF NOT EXISTS player_stats ("
                                + "uuid CHAR(36) PRIMARY KEY," + "player_name STRING,"
                                + "play_time INT," + "mobs_killed INT," + "items_eaten INT,"
                                + "distance_traveled DOUBLE," + "blocks_broken INT," + "deaths INT,"
                                + "items_crafted INT," + "items_used INT," + "chests_opened INT,"
                                + "messages_sent INT" + ")");
                stmt.execute();
                stmt.close();

                Bukkit.getLogger().info("Database storage reloaded successfully.");
            } catch (SQLException | ClassNotFoundException e) {
                Bukkit.getLogger().severe("Error reloading database storage");
                e.printStackTrace();
            }
        } else {
            Bukkit.getLogger()
                    .info("Database storage is already active and does not need to be reloaded.");
        }
    }

    @Override
    public void close() {
        saveAllPlayers();
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error closing database connection");
            e.printStackTrace();
        }
        Bukkit.getLogger().info("Database storage closed successfully.");
    }

    @Override
    public List<PlayerStats> getTopStats(StatType statType, int limit) {
        List<PlayerStats> statsList = loadAllPlayers();

        if (statsList == null)
            return null;

        // Фильтруем статистику по типу
        List<PlayerStats> filteredStats = new ArrayList<>();
        for (PlayerStats stats : statsList) {
            switch (statType) {
                case PLAY_TIME:
                    if (stats.getPlayTime() > 0)
                        filteredStats.add(stats);
                    break;
                case MOBS_KILLED:
                    if (stats.getMobsKilled() > 0)
                        filteredStats.add(stats);
                    break;
                case ITEMS_EATEN:
                    if (stats.getItemsEaten() > 0)
                        filteredStats.add(stats);
                case DISTANCE_TRAVELED:
                    if (stats.getDistanceTraveled() > 0)
                        filteredStats.add(stats);
                case BLOCKS_BROKEN:
                    if (stats.getBlocksBroken() > 0)
                        filteredStats.add(stats);
                case CHEST_OPENED:
                    if (stats.getChestsOpened() > 0)
                        filteredStats.add(stats);
            }
        }

        // Сортируем список по выбранному типу статистики
        switch (statType) {
            case PLAY_TIME:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getPlayTime).reversed());
                break;
            case MOBS_KILLED:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getMobsKilled).reversed());
                break;
            case ITEMS_EATEN:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getItemsEaten).reversed());
            case BLOCKS_BROKEN:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getBlocksBroken).reversed());
                break;
            case CHEST_OPENED:
                Collections.sort(filteredStats,
                        Comparator.comparingInt(PlayerStats::getChestsOpened).reversed());
                break;
            case DISTANCE_TRAVELED:
                Collections.sort(filteredStats,
                        Comparator.comparingDouble(PlayerStats::getDistanceTraveled).reversed());
                break;
            default:
                break;
        }

        if (limit > filteredStats.size()) {
            return filteredStats;
        }
        if (limit <= 0) {
            return filteredStats;
        }

        return filteredStats.subList(0, Math.min(filteredStats.size(), limit));
    }
}
