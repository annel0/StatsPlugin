package ru.annelo.player2statistic;

import io.papermc.lib.PaperLib;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsPlugin extends JavaPlugin {
    private IStorage storage;
    private Config config;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        PaperLib.suggestPaper(this);
        saveDefaultConfig();

        // Загрузка конфигурации
        config = new Config(this);

        // Инициализация системы хранения
        storage =
                config.isDatabase()
                        ? new DatabaseStorage(config)
                        : new FileStorage(this);

        statsManager = new StatsManager(this, storage);

        // Регистрация обработчиков событий
        getServer().getPluginManager().registerEvents(new StatsListener(statsManager, config), this);

        // Регистрация команды
        getCommand("stats").setExecutor(new StatsCommand(this, statsManager, config));

        // Запуск автосохранения
        if (config.isAutosaveEnabled()) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                statsManager.saveAllCached();
                getLogger().info("Данные успешно сохранены.");
            }, 0, config.getAutosaveInterval() * 20 * 60); // Интервал в тиках (20 тиков = 1 сек)
        }
    }

    @Override
    public void onDisable() {
        // Сохранение всех данных при выключении
        if (statsManager != null) {
            statsManager.saveAllCachedSync();
        }
        if (storage != null) {
            storage.close();
        }
    }
    
    public void setStorage(IStorage storage) {
        this.storage = storage;
        if (statsManager != null) {
            statsManager.setStorage(storage);
        }
    }

    public IStorage getStorage() {
        return this.storage;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }
}


// Обработчик игровых событий
class StatsListener implements Listener {
    private final StatsManager statsManager;
    private final Config config;

    public StatsListener(StatsManager statsManager, Config config) {
        this.statsManager = statsManager;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        statsManager.createEmptyStats(uuid, event.getPlayer().getName());
        statsManager.loadStats(uuid);

        if (config.isEnablePlayTime()) {
            statsManager.startSession(uuid);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (config.isEnablePlayTime()) {
            statsManager.endSession(uuid);
        }
        statsManager.unloadStats(uuid);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!config.isEnableKills())
            return;

        if (event.getEntity().getKiller() instanceof Player) {
            Player killer = (Player) event.getEntity().getKiller();
            statsManager.incrementMobsKilled(killer.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncChatEvent event) {
        if (!config.isEnableMessagesSent())
            return;
        statsManager.incrementMessagesSent(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!config.isEnableDistance())
            return;

        if (event.getTo() == null
                || !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            return;
        }

        double distance = event.getFrom().distance(event.getTo());
        if (distance > 0) {
            statsManager.addDistanceTraveled(event.getPlayer().getUniqueId(), distance);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Проверка на открытие сундука
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST
                && config.isEnableChestOpening()) {
             if (event.getAction().name().contains("RIGHT_CLICK")) {
                 statsManager.incrementChestsOpened(event.getPlayer().getUniqueId());
             }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (!config.isEnableFoodConsumption()) return;

        if (constants.edibleMaterials.contains(event.getItem().getType())) {
            statsManager.incrementItemsEaten(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (!config.isEnableBlockBreaking())
            return;
        statsManager.incrementBlocksBroken(event.getPlayer().getUniqueId());
    }
}
