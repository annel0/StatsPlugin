package ru.annelo.player2statistic;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;

public class StatsCommand implements CommandExecutor {

    private final IStorage storage;
    private final Config config;

    public StatsCommand(IStorage storage, Config config) {
        this.storage = storage;
        this.config = config;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            PlayerStats stats = null;

            if (!player.hasPermission("player2statistic.stats")) {
                player.sendMessage(
                        ChatColor.RED + "У вас нет прав для использования этой команды.");
            }
            if (args.length > 0) {
                if (!player.hasPermission("player2statistic.admin")) {
                    player.sendMessage(
                            ChatColor.RED + "У вас нет прав для использования этой команды.");
                    return true;
                }
                Player target = player.getServer().getPlayer(args[0]);
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Игрок не найден.");
                    return true;
                }
                stats = storage.loadPlayerStats(target.getUniqueId());
            } else {
                stats = storage.loadPlayerStats(player.getUniqueId());
            }

            if (stats == null) {
                player.sendMessage(ChatColor.RED + "Статистика не найдена.");
                return true;
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
        return false;
    }
}
