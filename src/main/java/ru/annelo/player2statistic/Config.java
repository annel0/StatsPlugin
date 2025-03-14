package ru.annelo.player2statistic;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;

public class Config {
    private FileConfiguration config;
    private final Logger logger;

    private JavaPlugin localPlugin;

    public Config(JavaPlugin plugin) {
        config = plugin.getConfig();
        config.options().copyDefaults(true);

        localPlugin = plugin;
        logger = plugin.getLogger();
    }

    // Геттеры для всех настроек из config.yml
    public boolean isAutosaveEnabled() {
        return config.getBoolean("autosave.enabled");
    }

    public int getAutosaveInterval() {
        return config.getInt("autosave.interval");
    }

    public boolean isFeatureEnabled(String feature) {
        return config.getBoolean("features." + feature + ".enabled");
    }

    public String getDisplayFormat() {
        return config.getString("display.format");
    }

    public boolean isDatabase() {
        return config.getString("database.type") == "database";
    }

    public String getDatabaseHost() {
        return config.getString("database.host");
    }

    public int getDatabasePort() {
        return config.getInt("database.port", 3306);
    }

    public String getDatabaseName() {
        return config.getString("database.name", "player2statistic");
    }

    public String getDatabaseUsername() {
        return config.getString("database.username");
    }

    public String getDatabasePassword() {
        return config.getString("database.password");
    }

    public String getLogPath() {
        return config.getString("log.path");
    }

    // Сохранение изменений в файле
    public void saveConfig(String file_name) {
        try {
            config.save(file_name);
        } catch (Exception e) {
            logger.severe(String.format("Ошибка сохранения файла %s", file_name));
        }
    }

    // Сохранение изменений в файле config.yml
    public void saveConfig() {
        try {
            config.save("config.yml");
        } catch (Exception e) {
            logger.severe("Ошибка сохранения файла config.yml");
        }
    }

    public boolean isEnablePlayTime() {
        return isFeatureEnabled("playTime");
    }

    public boolean isEnableKills() {
        return isFeatureEnabled("mobKilling");
    }

    public boolean isEnableDistance() {
        return isFeatureEnabled("movementTracking");
    }

    public boolean isEnableChestOpening() {
        return isFeatureEnabled("chestOpening");
    }

    public boolean isEnableFoodConsumption() {
        return isFeatureEnabled("foodConsumption");
    }

    public boolean isEnableBlockBreaking() {
        return isFeatureEnabled("blockBreaking");
    }

    public void reload() {
        config = localPlugin.getConfig();
    }

    public void setDatabase(boolean isDatabase) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDatabase'");
    }

    public void save() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }
}
