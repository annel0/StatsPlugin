package ru.annelo.player2statistic;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
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

    List<PlayerStats> getTopStats(StatType statType, int limit);
}


// Реализация локального хранилища (JSON)
class FileStorage implements IStorage {
    private final JavaPlugin plugin;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private boolean legacyMigrationAttempted = false;

    public FileStorage(JavaPlugin plugin2) {
        this.plugin = plugin2;
    }

    @Override
    public synchronized void savePlayerStats(PlayerStats stats) {
        try {
            File dataFolder = plugin.getDataFolder();
            File statsDir = new File(dataFolder, "stats");
            File statsFile = new File(statsDir, stats.getUuid().toString() + ".json");

            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            if (!statsDir.exists()) {
                statsDir.mkdirs();
            }

            JsonObject json = StatsJsonCodec.toJson(stats);

            try (FileWriter writer = new FileWriter(statsFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Error saving player stats for " + stats.getUuid());
            e.printStackTrace();
        }
    }

    @Override
    public synchronized PlayerStats loadPlayerStats(UUID uuid) {
        migrateLegacyJsonIfPresent();
        File dataFolder = plugin.getDataFolder();
        File statsFile = new File(dataFolder, "stats/" + uuid.toString() + ".json");
        File legacyStatsFile = new File(dataFolder, "stats/" + uuid.toString() + ".yml");

        if (!statsFile.exists()) {
            if (legacyStatsFile.exists()) {
                PlayerStats legacyStats = loadLegacyYamlStats(legacyStatsFile);
                if (legacyStats != null) {
                    savePlayerStats(legacyStats);
                    return legacyStats;
                }
            }
            PlayerStats newStats = new PlayerStats();
            newStats.setUuid(uuid);
            savePlayerStats(newStats);
            return newStats;
        }

        try {
            try (FileReader reader = new FileReader(statsFile)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return StatsJsonCodec.fromJson(json, uuid);
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Error loading player stats for " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public synchronized List<PlayerStats> loadAllPlayers() {
        migrateLegacyJsonIfPresent();
        File dataFolder = plugin.getDataFolder();
        File statsDir = new File(dataFolder, "stats");

        List<PlayerStats> statsList = new ArrayList<>();

        if (!statsDir.exists()) {
            statsDir.mkdir();
            return Collections.emptyList();
        }

        File[] files = statsDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }

        for (File file : files) {
            if (file.getName().endsWith(".json")) {
                UUID uuid = UUID.fromString(file.getName().replace(".json", ""));
                PlayerStats stats = loadPlayerStats(uuid);
                if (stats != null) {
                    statsList.add(stats);
                }
            }
            // Removed legacy yaml loading here to speed up, assuming they are migrated on individual load or separate migration.
            // Keeping it consistent with previous logic though:
             if (file.getName().endsWith(".yml")) {
                UUID uuid = UUID.fromString(file.getName().replace(".yml", ""));
                PlayerStats stats = loadLegacyYamlStats(file);
                if (stats != null) {
                    savePlayerStats(stats);
                    statsList.add(stats);
                }
            }
        }

        return statsList;
    }

    @Override
    public void saveAllPlayers() {
        // StatsManager handles periodic saving. This might be unused now except on close.
    }

    @Override
    public void reloadStorage() {
        // No-op for file storage usually
    }

    @Override
    public void close() {
        // Nothing to close
    }

    private PlayerStats loadLegacyYamlStats(File statsFile) {
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
            Bukkit.getLogger().severe("Error loading legacy YAML stats for " + statsFile.getName());
            e.printStackTrace();
            return null;
        }
    }

    private void migrateLegacyJsonIfPresent() {
        if (legacyMigrationAttempted) {
            return;
        }
        legacyMigrationAttempted = true;
        File dataFolder = plugin.getDataFolder();
        File legacyFile = new File(dataFolder, "stats.json");
        File statsDir = new File(dataFolder, "stats");
        if (!legacyFile.exists()) {
            return;
        }
        if (!statsDir.exists() && !statsDir.mkdirs()) {
            return;
        }

        try (FileReader reader = new FileReader(legacyFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                for (String key : obj.keySet()) {
                    JsonElement value = obj.get(key);
                    if (value != null && value.isJsonObject()) {
                        PlayerStats stats = StatsJsonCodec.fromJson(value.getAsJsonObject(),
                                UUID.fromString(key));
                        savePlayerStats(stats);
                    }
                }
            } else if (root.isJsonArray()) {
                for (JsonElement element : root.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.has("uuid")) {
                            PlayerStats stats =
                                    StatsJsonCodec.fromJson(obj, UUID.fromString(obj.get("uuid")
                                            .getAsString()));
                            savePlayerStats(stats);
                        }
                    }
                }
            }
            File backupFile = new File(dataFolder, "stats.json.bak");
            legacyFile.renameTo(backupFile);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Ошибка миграции legacy stats.json");
            e.printStackTrace();
        }
    }

    @Override
    public List<PlayerStats> getTopStats(StatType statType, int limit) {
        // Warning: Loading all players into memory!
        List<PlayerStats> statsList = loadAllPlayers();

        if (statsList == null)
            return Collections.emptyList();

        List<PlayerStats> filteredStats = new ArrayList<>();
        for (PlayerStats stats : statsList) {
             boolean add = false;
             switch (statType) {
                case PLAY_TIME: add = stats.getPlayTime() > 0; break;
                case MOBS_KILLED: add = stats.getMobsKilled() > 0; break;
                case ITEMS_EATEN: add = stats.getItemsEaten() > 0; break;
                case DISTANCE_TRAVELED: add = stats.getDistanceTraveled() > 0; break;
                case BLOCKS_BROKEN: add = stats.getBlocksBroken() > 0; break;
                case CHEST_OPENED: add = stats.getChestsOpened() > 0; break;
            }
            if (add) filteredStats.add(stats);
        }

        switch (statType) {
            case PLAY_TIME:
                Collections.sort(filteredStats, Comparator.comparingInt(PlayerStats::getPlayTime).reversed());
                break;
            case MOBS_KILLED:
                Collections.sort(filteredStats, Comparator.comparingInt(PlayerStats::getMobsKilled).reversed());
                break;
            case ITEMS_EATEN:
                Collections.sort(filteredStats, Comparator.comparingInt(PlayerStats::getItemsEaten).reversed());
                break;
            case BLOCKS_BROKEN:
                Collections.sort(filteredStats, Comparator.comparingInt(PlayerStats::getBlocksBroken).reversed());
                break;
            case CHEST_OPENED:
                Collections.sort(filteredStats, Comparator.comparingInt(PlayerStats::getChestsOpened).reversed());
                break;
            case DISTANCE_TRAVELED:
                Collections.sort(filteredStats, Comparator.comparingDouble(PlayerStats::getDistanceTraveled).reversed());
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
    private HikariDataSource dataSource;
    private Config config;

    public DatabaseStorage(Config config) {
        this.config = config;
        initialize();
    }

    private void initialize() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mariadb://" + config.getDatabaseHost() + ":"
                + config.getDatabasePort() + "/" + config.getDatabaseName());
        hikariConfig.setUsername(config.getDatabaseUsername());
        hikariConfig.setPassword(config.getDatabasePassword());
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setConnectionTimeout(10000);

        dataSource = new HikariDataSource(hikariConfig);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS player_stats (" + "uuid CHAR(36) PRIMARY KEY,"
                            + "player_name VARCHAR(64)," + "play_time INT," + "mobs_killed INT,"
                            + "items_eaten INT," + "distance_traveled DOUBLE,"
                            + "blocks_broken INT," + "deaths INT," + "items_crafted INT,"
                            + "items_used INT," + "chests_opened INT," + "messages_sent INT" + ")")) {
            stmt.execute();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error connecting to database");
            e.printStackTrace();
        }
    }

    @Override
    public void savePlayerStats(PlayerStats stats) {
        try (Connection connection = dataSource.getConnection();
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
                        + "messages_sent = VALUES(messages_sent)")) {

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
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error saving player stats for " + stats.getUuid());
            e.printStackTrace();
        }
    }

    @Override
    public PlayerStats loadPlayerStats(UUID uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSet(rs);
                } else {
                    PlayerStats newStats = new PlayerStats();
                    newStats.setUuid(uuid);
                    savePlayerStats(newStats);
                    return newStats;
                }
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error loading player stats for " + uuid);
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<PlayerStats> loadAllPlayers() {
        // This operation is very heavy.
        // But implementation is needed for interface.
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT * FROM player_stats");
             ResultSet rs = stmt.executeQuery()) {

            List<PlayerStats> statsList = new ArrayList<>();
            while (rs.next()) {
                statsList.add(mapResultSet(rs));
            }
            return statsList;
        } catch (SQLException e) {
            Bukkit.getLogger().severe("Error loading all player stats");
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @Override
    public void saveAllPlayers() {
        // Auto-saved
    }

    @Override
    public void reloadStorage() {
        close();
        initialize();
        Bukkit.getLogger().info("Database storage reloaded successfully.");
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        Bukkit.getLogger().info("Database storage closed successfully.");
    }

    @Override
    public List<PlayerStats> getTopStats(StatType statType, int limit) {
        String column = "";
        switch (statType) {
            case PLAY_TIME: column = "play_time"; break;
            case MOBS_KILLED: column = "mobs_killed"; break;
            case ITEMS_EATEN: column = "items_eaten"; break;
            case DISTANCE_TRAVELED: column = "distance_traveled"; break;
            case BLOCKS_BROKEN: column = "blocks_broken"; break;
            case CHEST_OPENED: column = "chests_opened"; break;
            default: return Collections.emptyList();
        }

        List<PlayerStats> statsList = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM player_stats ORDER BY " + column + " DESC LIMIT ?")) {
             stmt.setInt(1, limit);
             try (ResultSet rs = stmt.executeQuery()) {
                 while (rs.next()) {
                     statsList.add(mapResultSet(rs));
                 }
             }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return statsList;
    }

    private PlayerStats mapResultSet(ResultSet rs) throws SQLException {
        PlayerStats stats = new PlayerStats();
        stats.setUuid(UUID.fromString(rs.getString("uuid")));
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
        return stats;
    }
}
