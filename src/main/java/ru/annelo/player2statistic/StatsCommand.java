package ru.annelo.player2statistic;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.md_5.bungee.api.ChatColor;

public class StatsCommand implements CommandExecutor {

        private IStorage storage;
        private final Config config;
        private final StatsPlugin plugin;
        
        public StatsCommand(StatsPlugin plugin, IStorage storage, Config config) {
            this.storage = storage;
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
                    plugin.setStorage(config.isDatabase()
                        ? new DatabaseStorage(config)
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
        Player target = player.getServer().getPlayer(args[1]);

        if (!player.hasPermission("player2statistic.stats")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав для использования этой команды.");
            return false;
        }

        PlayerStats stats = storage.loadPlayerStats(target.getUniqueId());
        if (stats == null) {
            player.sendMessage(ChatColor.RED + "Статистика не найдена.");
            return false;
        }

        String format = config.getDisplayFormat();
        format = format.replace("{play_time}", String.valueOf(stats.getPlayTime()))
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

        String newDbType = args[1];
        if (!newDbType.equals("file") && !newDbType.equals("database")) {
        player.sendMessage(ChatColor.RED + "Недопустимый тип базы данных. Используйте \"file\" или \"database\".");
            return false;
        }

        boolean isDatabase = newDbType.equals("database");
        if (isDatabase == config.isDatabase()) {
            player.sendMessage(ChatColor.RED + "Текущий тип базы данных уже установлен.");
            return false;
        }

        try {
            IStorage newStorage = isDatabase
                    ? new DatabaseStorage(config)
                    : new FileStorage(plugin);
            storage.close();
            storage = newStorage;
            config.setDatabase(isDatabase);
            config.save();
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Ошибка при переключении базы данных.");
            e.printStackTrace();
            return false;
        }
        player.sendMessage(ChatColor.GREEN + "База данных успешно переключена на " + (isDatabase ? "базу данных" : "файловую систему."));
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
        int limit = args.length > 2 ? Integer.parseInt(args[2]) : 10;

        List<PlayerStats> topPlayers;

        switch (type) {
            case "play_time":
                topPlayers = storage.getTopStats(StatType.PLAY_TIME, limit);
                player.sendMessage(ChatColor.GOLD + "Топ игроков по времени игры:");
                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    String playerName = plugin.getServer().getOfflinePlayer(stats.getUuid()).getName();
                    playerName = playerName == null ? "Неизвестный игрок" : playerName;

                    player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " " + playerName + " - " + stats.getPlayTime() + " секунд");
                }
                break;
            case "blocks_broken":
                topPlayers = storage.getTopStats(StatType.BLOCKS_BROKEN, limit);
                player.sendMessage(ChatColor.GOLD + "Топ игроков по количеству сломанных блоков:");
                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    String playerName = plugin.getServer().getOfflinePlayer(stats.getUuid()).getName();
                    playerName = playerName == null ? "Неизвестный игрок" : playerName;

                    player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " " + playerName + " - " + stats.getBlocksBroken() + " блоков");
                }
                break;
            case "mobs_killed":
                topPlayers = storage.getTopStats(StatType.MOBS_KILLED, limit);
                player.sendMessage(ChatColor.GOLD + "Топ игроков по количеству убитых мобов:");
                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    String playerName = plugin.getServer().getOfflinePlayer(stats.getUuid()).getName();
                    playerName = playerName == null ? "Неизвестный игрок" : playerName;

                    player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " " + playerName + " - " + stats.getMobsKilled() + " мобов");
                }
                break;
            case "chests_opened":
                topPlayers = storage.getTopStats(StatType.CHEST_OPENED, limit);
                player.sendMessage(ChatColor.GOLD + "Топ игроков по количеству открытий сундуков:");
                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    String playerName = plugin.getServer().getOfflinePlayer(stats.getUuid()).getName();
                    playerName = playerName == null ? "Неизвестный игрок" : playerName;

                    player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " " + playerName + " - " + stats.getChestsOpened() + " сундуков");
                }
                break;
            case "items_eaten":
                topPlayers = storage.getTopStats(StatType.ITEMS_EATEN, limit);
                player.sendMessage(ChatColor.GOLD + "Топ игроков по количеству съеденных еды:");
                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    String playerName = plugin.getServer().getOfflinePlayer(stats.getUuid()).getName();
                    playerName = playerName == null ? "Неизвестный игрок" : playerName;

                    player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " " + playerName + " - " + stats.getItemsEaten() + " еды");
                }
                break;
            case "distance_traveled":
                topPlayers = storage.getTopStats(StatType.DISTANCE_TRAVELED, limit);
                player.sendMessage(ChatColor.GOLD + "Топ игроков по количеству пройденного пути:");
                for (int i = 0; i < topPlayers.size(); i++) {
                    PlayerStats stats = topPlayers.get(i);

                    String playerName = plugin.getServer().getOfflinePlayer(stats.getUuid()).getName();
                    playerName = playerName == null ? "Неизвестный игрок" : playerName;

                    player.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + " " + playerName + " - " + stats.getDistanceTraveled() + " метров");
                }
                break;
            
            default:
                player.sendMessage(ChatColor.RED + "Неверный тип статистики. Доступные типы: play_time, blocks_broken, mobs_killed, chests_opened, items_eaten, distance_traveled");
            
        }

        return true;
    }
}
