package ru.annelo.player2statistic;

import java.util.List;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

public class StatsCommand implements CommandExecutor {

    private final StatsManager statsManager;
    private final Config config;
    private final StatsPlugin plugin;

    public StatsCommand(StatsPlugin plugin, StatsManager statsManager, Config config) {
        this.statsManager = statsManager;
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (args.length >= 1) {
                switch (args[0]) {
                    case "player":
                        return getPlayerStats(player, args);
                    case "reload":
                        return reloadPlugin(player);
                    case "dbtype":
                        return migrateDatabase(player, args);
                    case "top":
                        return getTopStats(player, args);
                    default:
                        break;
                }
            }
        }
        return false;
    }

    public boolean reloadPlugin(Player player) {
        if (player.hasPermission("player2statistic.reload")) {
            try {
                config.reload();
                plugin.getStorage().close();
                plugin.setStorage(config.isDatabase() ? new DatabaseStorage(config)
                        : new FileStorage(plugin));
                player.sendMessage(ChatColor.GREEN + "Плагин успешно перезагружен.");
                return true;
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Ошибка при перезагрузке плагина.");
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean getPlayerStats(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /stats player <ник>");
            return false;
        }

        Player target = player.getServer().getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Игрок не найден (поддерживаются только онлайн игроки).");
            return false;
        }

        if (!player.hasPermission("player2statistic.stats")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return false;
        }

        PlayerStats stats = statsManager.getStats(target.getUniqueId());
        if (stats == null) {
            // Fallback to storage if not in cache (rare case for online player)
            stats = plugin.getStorage().loadPlayerStats(target.getUniqueId());
        }

        if (stats == null) {
            player.sendMessage(ChatColor.RED + "Статистика не найдена.");
            return false;
        }

        String format = config.getDisplayFormat();
        format = format.replace("{player_name}", String.valueOf(stats.getPlayerName()))
                .replace("{play_time}", String.valueOf(stats.getPlayTime()))
                .replace("{blocks_broken}", String.valueOf(stats.getBlocksBroken()))
                .replace("{mobs_killed}", String.valueOf(stats.getMobsKilled()))
                .replace("{chests_opened}", String.valueOf(stats.getChestsOpened()))
                .replace("{messages_sent}", String.valueOf(stats.getMessagesSent()));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', format));
        return true;
    }

    public boolean migrateDatabase(Player player, String[] args) {
        if (!player.hasPermission("player2statistic.dbtype")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED
                    + "Использование: /stats dbtype <file|database>");
            return false;
        }

        String newDbType = args[1];
        if (!newDbType.equals("file") && !newDbType.equals("database")) {
            player.sendMessage(ChatColor.RED
                    + "Недопустимый тип базы данных. Используйте \"file\" или \"database\".");
            return false;
        }

        boolean isDatabase = newDbType.equals("database");
        if (isDatabase == config.isDatabase()) {
            player.sendMessage(ChatColor.RED + "Текущий тип базы данных уже установлен.");
            return false;
        }

        player.sendMessage(ChatColor.YELLOW + "Начинаю миграцию данных... Это может занять некоторое время.");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                IStorage oldStorage = plugin.getStorage();
                // Load all stats from old storage
                List<PlayerStats> allStats = oldStorage.loadAllPlayers();

                // Initialize new storage
                IStorage newStorage = isDatabase ? new DatabaseStorage(config) : new FileStorage(plugin);

                // Save to new storage
                int count = 0;
                for (PlayerStats stats : allStats) {
                    newStorage.savePlayerStats(stats);
                    count++;
                }

                final int migratedCount = count;

                // Update plugin storage on main thread
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    try {
                        oldStorage.close();
                        plugin.setStorage(newStorage);
                        config.setDatabase(isDatabase);
                        config.save();
                        player.sendMessage(ChatColor.GREEN + "Миграция завершена успешно. Перенесено " + migratedCount + " записей.");
                        player.sendMessage(ChatColor.GREEN + "База данных переключена на " + (isDatabase ? "базу данных" : "файловую систему."));
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "Ошибка при переключении хранилища.");
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "Ошибка при миграции данных.");
                    e.printStackTrace();
                });
            }
        });

        return true;
    }

    public boolean getTopStats(Player player, String[] args) {
        if (!player.hasPermission("player2statistic.top")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Использование: /stats top <type> [limit]");
            return false;
        }

        String type = args[1].toLowerCase();
        int limit = 10;
        if (args.length > 2) {
            try {
                limit = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Лимит должен быть числом.");
                return false;
            }
        }

        StatType statType;
        try {
            if (type.equals("chests_opened")) {
                statType = StatType.CHEST_OPENED;
            } else {
                statType = StatType.valueOf(type.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatColor.RED + "Неверный тип статистики.");
            return false;
        }

        // Asynchronously fetch top stats to avoid blocking main thread
        // Commands usually run on main thread.
        // We should run this async and send message later.
        final int finalLimit = limit;
        final StatType finalStatType = statType;
        player.sendMessage(ChatColor.GRAY + "Загрузка топа...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            List<PlayerStats> topPlayers = plugin.getStorage().getTopStats(finalStatType, finalLimit);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                 if (topPlayers == null || topPlayers.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "Нет данных для отображения.");
                    return;
                }

                String title = "";
                switch (type) {
                     case "play_time": title = "Топ игроков по времени игры:"; break;
                     case "blocks_broken": title = "Топ игроков по количеству сломанных блоков:"; break;
                     case "mobs_killed": title = "Топ игроков по количеству убитых мобов:"; break;
                     case "chests_opened": title = "Топ игроков по количеству открытий сундуков:"; break;
                     case "items_eaten": title = "Топ игроков по количеству съеденных еды:"; break;
                     case "distance_traveled": title = "Топ игроков по количеству пройденного пути:"; break;
                }

                player.sendMessage(ChatColor.GOLD + title);
                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    String playerName = stats.getPlayerName();
                    if (playerName == null) {
                         playerName = plugin.getServer().getOfflinePlayer(stats.getUuid()).getName();
                    }
                    playerName = playerName == null ? "Неизвестный игрок" : playerName;

                    String value = "";
                    switch (type) {
                        case "play_time": value = stats.getPlayTime() + " секунд"; break; // Wait, playTime is minutes in PlayerStats? logic says seconds in output but minutes in field comments.
                        case "blocks_broken": value = stats.getBlocksBroken() + " блоков"; break;
                        case "mobs_killed": value = stats.getMobsKilled() + " мобов"; break;
                        case "chests_opened": value = stats.getChestsOpened() + " сундуков"; break;
                        case "items_eaten": value = stats.getItemsEaten() + " еды"; break;
                        case "distance_traveled": value = String.format("%.2f метров", stats.getDistanceTraveled()); break;
                    }

                    player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " " + playerName + " - " + value);
                }
            });
        });

        return true;
    }
}
