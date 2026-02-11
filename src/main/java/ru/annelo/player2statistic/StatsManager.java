package ru.annelo.player2statistic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;

public class StatsManager {
    private final StatsPlugin plugin;
    private IStorage storage;
    private final Map<UUID, PlayerStats> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> sessionStartTimes = new ConcurrentHashMap<>();

    public StatsManager(StatsPlugin plugin, IStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void setStorage(IStorage storage) {
        this.storage = storage;
    }

    public PlayerStats getStats(UUID uuid) {
        return cache.get(uuid);
    }

    public void createEmptyStats(UUID uuid, String playerName) {
        PlayerStats stats = new PlayerStats();
        stats.setUuid(uuid);
        stats.setPlayerName(playerName);
        cache.put(uuid, stats);
    }

    public void loadStats(UUID uuid) {
        // Load asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerStats dbStats = storage.loadPlayerStats(uuid);
            if (dbStats != null) {
                PlayerStats cached = cache.get(uuid);
                if (cached != null) {
                    synchronized (cached) {
                        cached.merge(dbStats);
                    }
                } else {
                    cache.put(uuid, dbStats);
                }
            }
        });
    }

    public void unloadStats(UUID uuid) {
        PlayerStats stats = cache.remove(uuid);
        if (stats != null) {
            saveStats(stats);
        }
    }

    public void saveAllCached() {
        for (PlayerStats stats : cache.values()) {
            saveStats(stats);
        }
    }

    public void startSession(UUID uuid) {
        sessionStartTimes.put(uuid, System.currentTimeMillis());
    }

    public void endSession(UUID uuid) {
        Long start = sessionStartTimes.remove(uuid);
        if (start != null) {
            int minutes = (int) ((System.currentTimeMillis() - start) / 60000);
            if (minutes > 0) {
                addPlayTime(uuid, minutes);
            }
        }
    }

    private void updateSessionStats() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, Long> entry : sessionStartTimes.entrySet()) {
            int minutes = (int) ((now - entry.getValue()) / 60000);
            if (minutes > 0) {
                addPlayTime(entry.getKey(), minutes);
                entry.setValue(now);
            }
        }
    }

    public void saveAllCachedSync() {
        updateSessionStats();
        for (PlayerStats stats : cache.values()) {
            final PlayerStats snapshot;
            synchronized (stats) {
                snapshot = stats.clone();
            }
            storage.savePlayerStats(snapshot);
        }
    }

    public void saveStats(PlayerStats stats) {
        final PlayerStats snapshot;
        synchronized (stats) {
            snapshot = stats.clone();
        }

        // Ensure saving is done asynchronously
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
             storage.savePlayerStats(snapshot);
        });
    }

    public void incrementMobsKilled(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setMobsKilled(stats.getMobsKilled() + 1);
            }
        }
    }

    public void incrementItemsEaten(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setItemsEaten(stats.getItemsEaten() + 1);
            }
        }
    }

    public void incrementBlocksBroken(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setBlocksBroken(stats.getBlocksBroken() + 1);
            }
        }
    }

    public void incrementDeaths(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setDeaths(stats.getDeaths() + 1);
            }
        }
    }

    public void incrementItemsCrafted(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setItemsCrafted(stats.getItemsCrafted() + 1);
            }
        }
    }

    public void incrementItemsUsed(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setItemsUsed(stats.getItemsUsed() + 1);
            }
        }
    }

    public void incrementChestsOpened(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setChestsOpened(stats.getChestsOpened() + 1);
            }
        }
    }

    public void incrementMessagesSent(UUID uuid) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setMessagesSent(stats.getMessagesSent() + 1);
            }
        }
    }

    public void addDistanceTraveled(UUID uuid, double distance) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setDistanceTraveled(stats.getDistanceTraveled() + distance);
            }
        }
    }

    public void addPlayTime(UUID uuid, int minutes) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setPlayTime(stats.getPlayTime() + minutes);
            }
        }
    }

    public void setPlayerName(UUID uuid, String name) {
        PlayerStats stats = cache.get(uuid);
        if (stats != null) {
            synchronized (stats) {
                stats.setPlayerName(name);
            }
        }
    }
}
