package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

public class PlayTime implements CommandExecutor {
    private final JavaPlugin plugin;
    private static HashMap<UUID, Long> playTimes = new HashMap<UUID, Long>();
    private final File playtimeDataFolder;

    public PlayTime(JavaPlugin plugin) {
        this.plugin = plugin;
        PlayTime.playTimes = new HashMap<>();
        this.playtimeDataFolder = new File(plugin.getDataFolder(), "players");
        if (!playtimeDataFolder.exists()) {
            playtimeDataFolder.mkdirs();
        }
        loadPlayTimes();
        updatePlayTimesTask();
        plugin.getCommand("playtime").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("pssu.playtime")) {
            sender.sendMessage("§cYou don't have permission to execute this command.");
            return true;
        }

        boolean hoursOnly = false;
        boolean minutesOnly = false;
        boolean secondsOnly = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-h")) {
                hoursOnly = true;
            } else if (args[i].equalsIgnoreCase("-m")) {
                minutesOnly = true;
            } else if (args[i].equalsIgnoreCase("-s")) {
                secondsOnly = true;
            }
        }

        // Remove the options from args
        args = Arrays.stream(args)
                .filter(arg -> !arg.equalsIgnoreCase("-h") && !arg.equalsIgnoreCase("-m") && !arg.equalsIgnoreCase("-s"))
                .toArray(String[]::new);

        if (args.length == 0) {
            // No argument provided, show sender's playtime
            if (sender instanceof Player) {
                Player player = (Player) sender;
                showPlayTime(player.getUniqueId(), sender, hoursOnly, minutesOnly, secondsOnly);
            } else {
                sender.sendMessage("Usage: /playtime [username]");
            }
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("top")) {
                // Show top 10 players by playtime
                showTopPlayTimes(sender, hoursOnly, minutesOnly, secondsOnly);
            } else {
                // Show playtime of the specified player
                String targetUsername = args[0];
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(targetUsername);
                if (offlinePlayer.hasPlayedBefore()) {
                    UUID playerUUID = offlinePlayer.getUniqueId();
                    showPlayTime(playerUUID, sender, hoursOnly, minutesOnly, secondsOnly);
                } else {
                    sender.sendMessage("Player: " + offlinePlayer.getName() + " could not be found.");
                }
            }
        } else {
            sender.sendMessage("Usage: /playtime [username]");
        }
        return true;
    }

    private void showPlayTime(UUID playerUUID, CommandSender sender, boolean hoursOnly, boolean minutesOnly, boolean secondsOnly) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
        if (offlinePlayer != null && playTimes.containsKey(playerUUID)) {
            long playTime = playTimes.get(playerUUID);
            if (hoursOnly) {
                long hours = playTime / 3600; // Convert playtime to hours
                sender.sendMessage("Player: " + offlinePlayer.getName());
                sender.sendMessage("Hours: " + hours);
            } else if (minutesOnly) {
                long minutes = playTime / 60; // Convert playtime to minutes
                sender.sendMessage("Player: " + offlinePlayer.getName());
                sender.sendMessage("Minutes: " + minutes);
            } else if (secondsOnly) {
                sender.sendMessage("Player: " + offlinePlayer.getName());
                sender.sendMessage("Seconds: " + playTime);
            } else {
                long days = playTime / 86400; // Seconds in a day
                long hours = (playTime % 86400) / 3600; // Seconds in an hour
                long minutes = (playTime % 3600) / 60; // Seconds in a minute
                long seconds = playTime % 60;

                sender.sendMessage("Player: " + offlinePlayer.getName());
                sender.sendMessage("Days: " + days);
                sender.sendMessage("Hours: " + hours);
                sender.sendMessage("Minutes: " + minutes);
                sender.sendMessage("Seconds: " + seconds);
            }
        } else {
            sender.sendMessage("Player: " + offlinePlayer.getName() + " could not be found.");
        }
    }

    private void showTopPlayTimes(CommandSender sender, boolean hoursOnly, boolean minutesOnly, boolean secondsOnly) {
        sender.sendMessage("Top 10 players playtime:");

        // Sort players by playtime in descending order
        Map<UUID, Long> sortedPlayTimes = playTimes.entrySet()
                .stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int rank = 1;
        for (Map.Entry<UUID, Long> entry : sortedPlayTimes.entrySet()) {
            UUID playerUUID = entry.getKey();
            long playTime = entry.getValue();
            if (hoursOnly) {
                long hours = playTime / 3600; // Convert playtime to hours
                sender.sendMessage(rank + ". " + Bukkit.getOfflinePlayer(playerUUID).getName() + ": " + hours + " hours");
            } else if (minutesOnly) {
                long minutes = playTime / 60; // Convert playtime to minutes
                sender.sendMessage(rank + ". " + Bukkit.getOfflinePlayer(playerUUID).getName() + ": " + minutes + " minutes");
            } else if (secondsOnly) {
                sender.sendMessage(rank + ". " + Bukkit.getOfflinePlayer(playerUUID).getName() + ": " + playTime + " seconds");
            } else {
                long days = playTime / 86400;
                long hours = (playTime % 86400) / 3600;
                long minutes = (playTime % 3600) / 60;
                long seconds = playTime % 60;

                sender.sendMessage(rank + ". " + Bukkit.getOfflinePlayer(playerUUID).getName() + ": " + days + "d " + hours + "h " + minutes + "m " + seconds + "s");
            }
            rank++;
        }
    }

    private void loadPlayTimes() {
        File[] playerFiles = playtimeDataFolder.listFiles();
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                if (playerFile.isFile()) {
                    String fileName = playerFile.getName();
                    if (fileName.endsWith(".yml")) {
                        String uuidString = fileName.substring(0, fileName.length() - 4);
                        UUID playerUUID = UUID.fromString(uuidString);
                        FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                        long playTime = playerConfig.getLong("playtime", 0L);
                        playTimes.put(playerUUID, playTime);
                    }
                }
            }
        }
    }

    private void updatePlayTimesTask() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                UUID playerUUID = player.getUniqueId();
                long lastPlayTime = playTimes.getOrDefault(playerUUID, 0L) + 1;
                playTimes.put(playerUUID, lastPlayTime);

                // Save playtime to individual player file
                File playerFile = new File(playtimeDataFolder, playerUUID.toString() + ".yml");
                FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                playerConfig.set("playtime", lastPlayTime);
                try {
                    playerConfig.save(playerFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 20); // 20 ticks = 1 second
    }
    static long getPlaytime(UUID playerUUID) {
        return playTimes.getOrDefault(playerUUID, 0L);
    }
}
