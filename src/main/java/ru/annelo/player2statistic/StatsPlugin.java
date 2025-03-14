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

        // Регистрация обработчиков событий
        getServer().getPluginManager().registerEvents(new StatsListener(storage, config), this);

        // Регистрация команды
        getCommand("stats").setExecutor(new StatsCommand(this, storage, config));

        // Запуск автосохранения
        if (config.isAutosaveEnabled()) {
            getServer().getScheduler().runTaskTimer(this, () -> {
                storage.saveAllPlayers();
                getLogger().info("Данные успешно сохранены.");
            }, 0, config.getAutosaveInterval() * 20 * 60); // Интервал в тиках (20 тиков = 1 сек)
        }

        // Загрузка данных при старте сервера
        storage.loadAllPlayers();
    }

    @Override
    public void onDisable() {
        // Сохранение всех данных при выключении
        if (storage != null) {
            storage.saveAllPlayers();
        }
    }
    
    public void setStorage(IStorage storage) {
        this.storage = storage;
    }

    public IStorage getStorage() {
        return this.storage;
    }
}


// Обработчик игровых событий
class StatsListener implements Listener {
    private final IStorage storage;
    private final Config config;

    private final Map<UUID, Long> playTimeMap = new HashMap<>();
    // private final Map<UUID, Integer> lastItemEatMap = new HashMap<>();
    // private final Map<UUID, Integer> lastBlockBreakMap = new HashMap<>();
    // private final Map<UUID, Integer> lastChestOpenMap = new HashMap<>();
    // private final Map<UUID, Integer> lastMessageSendMap = new HashMap<>();

    public StatsListener(IStorage storage2, Config config) {
        this.storage = storage2;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!config.isEnablePlayTime())
            return;
        // Запуск таймера игрового времени
        playTimeMap.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!config.isEnablePlayTime())
            return;
        // Сохранение игрового времени и других данных
        UUID uuid = event.getPlayer().getUniqueId();
        if (playTimeMap.containsKey(uuid)) {
            long playTime = (System.currentTimeMillis() - playTimeMap.get(uuid)) / 60000;
            PlayerStats stats = storage.loadPlayerStats(uuid);
            stats.setPlayTime(stats.getPlayTime() + (int) playTime);
            storage.savePlayerStats(stats);
            playTimeMap.remove(uuid);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!config.isEnableKills())
            return;

        if (event.getEntity().getKiller() instanceof Player) {
            // Увеличение счетчика убитых мобов
            Player killer = (Player) event.getEntity().getKiller();
            PlayerStats stats = storage.loadPlayerStats(killer.getUniqueId());
            stats.setMobsKilled(stats.getMobsKilled() + 1);
            storage.savePlayerStats(stats);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncChatEvent event) {
        if (!config.isEnableKills())
            return;

        UUID uuid = event.getPlayer().getUniqueId();
        PlayerStats stats = storage.loadPlayerStats(uuid);
        stats.setMessagesSent(stats.getMessagesSent() + 1);
        storage.savePlayerStats(stats);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!config.isEnableDistance())
            return;

        UUID uuid = event.getPlayer().getUniqueId();
        PlayerStats stats = storage.loadPlayerStats(uuid);
        double distance = event.getFrom().distance(event.getTo());
        stats.setDistanceTraveled(stats.getDistanceTraveled() + distance);
        storage.savePlayerStats(stats);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        PlayerStats stats = storage.loadPlayerStats(uuid);

        // Проверка на правый клик и едимость предмета
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            ItemStack item = event.getItem();
            if (item != null && constants.edibleMaterials.contains(item.getType())
                    && config.isEnableFoodConsumption()) {
                stats.setItemsEaten(stats.getItemsEaten() + 1);
                storage.savePlayerStats(stats);
            }
        }

        // Проверка на открытие сундука
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() == Material.CHEST
                && config.isEnableChestOpening()) {
            stats.setChestsOpened(stats.getChestsOpened() + 1);
            storage.savePlayerStats(stats);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (!config.isEnableBlockBreaking())
            return;

        UUID uuid = event.getPlayer().getUniqueId();
        PlayerStats stats = storage.loadPlayerStats(uuid);
        stats.setBlocksBroken(stats.getBlocksBroken() + 1);
        storage.savePlayerStats(stats);
    }
}
