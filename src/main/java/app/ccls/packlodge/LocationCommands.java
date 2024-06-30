package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class LocationCommands implements CommandExecutor {

    private final File playtimeDataFolder;
    private final HashMap<UUID, YamlConfiguration> playerData;

    public LocationCommands(Main plugin) {
        this.playtimeDataFolder = new File(plugin.getDataFolder(), "players");
        if (!playtimeDataFolder.exists()) {
            playtimeDataFolder.mkdirs();
        }
        this.playerData = new HashMap<>();

        loadPlayerData();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();

        if (!player.hasPermission("pssu.location")) {
            player.sendMessage("You don't have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("location")) {
            if (args.length == 0) {
                player.sendMessage("Usage: /location save <name>, /location list, /location del <name>, /location rename <name> <new-name>");
                return true;
            }

            if (args[0].equalsIgnoreCase("save")) {
                if (args.length < 2) {
                    player.sendMessage("Usage: /location save <name>");
                    return true;
                }
                String name = args[1];
                saveLocation(playerUUID, name, player.getLocation());
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                listLocations(playerUUID, player);
                return true;
            }

            if (args[0].equalsIgnoreCase("del")) {
                if (args.length < 2) {
                    player.sendMessage("Usage: /location del <name>");
                    return true;
                }
                String name = args[1];
                deleteLocation(playerUUID, name, sender);
                return true;
            }

            if (args.length == 3 && args[0].equalsIgnoreCase("rename")) {
                String name = args[1];
                String newName = args[2];
                renameLocation(playerUUID, name, newName, sender);
                return true;
            }
        }
        return false;
    }

    private void loadPlayerData() {
        File[] playerFiles = playtimeDataFolder.listFiles();
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                if (playerFile.isFile()) {
                    UUID playerUUID = UUID.fromString(playerFile.getName().replace(".yml", ""));
                    YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                    playerData.put(playerUUID, playerConfig);
                }
            }
        }
    }
    
    private void savePlayerData(UUID playerUUID) {
        if (playerData.containsKey(playerUUID)) {
            YamlConfiguration playerConfig = playerData.get(playerUUID);
            File playerFile = new File(playtimeDataFolder, playerUUID.toString() + ".yml");
            try {
                playerConfig.save(playerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    void saveLocation(UUID playerUUID, String name, org.bukkit.Location location) {
        long playtime = PlayTime.getPlaytime(playerUUID);
    
        YamlConfiguration playerConfig = playerData.computeIfAbsent(playerUUID, k -> new YamlConfiguration());
        ConfigurationSection locationsSection = playerConfig.getConfigurationSection("locations");
        if (locationsSection == null) {
            locationsSection = playerConfig.createSection("locations");
        }
        playerConfig.set("player", Bukkit.getPlayer(playerUUID).getName());
        playerConfig.set("playtime", playtime);
        locationsSection.set(name, formatLocation(location));
        savePlayerData(playerUUID);
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            player.sendMessage("Successfully saved location '" + name + "'.");
        }
    }
    
    public void listLocations(UUID playerUUID, CommandSender sender) {
        sender.sendMessage("List of saved locations:");
        if (playerData.containsKey(playerUUID)) {
            YamlConfiguration playerConfig = playerData.get(playerUUID);
            ConfigurationSection locationsSection = playerConfig.getConfigurationSection("locations");
            if (locationsSection != null) {
                for (String key : locationsSection.getKeys(false)) {
                    String locationInfo = locationsSection.getString(key);
                    sender.sendMessage(key + " " + locationInfo);
                }
            }
        } else {
            sender.sendMessage("You have no saved locations.");
        }
    }
    
    void deleteLocation(UUID playerUUID, String name, CommandSender sender) {
        if (playerData.containsKey(playerUUID)) {
            YamlConfiguration playerConfig = playerData.get(playerUUID);
            ConfigurationSection locationsSection = playerConfig.getConfigurationSection("locations");
            if (locationsSection != null && locationsSection.contains(name)) {
                locationsSection.set(name, null);
                savePlayerData(playerUUID);
                sender.sendMessage("Location '" + name + "' has been successfully deleted.");
            } else {
                sender.sendMessage("Location '" + name + "' does not exist.");
            }
        }
    }
    
    private String formatLocation(org.bukkit.Location location) {
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        String dimension = location.getWorld().getEnvironment().name();
        return String.format("(X: %.2f, Y: %.2f, Z: %.2f) (%s)", x, y, z, dimension);
    }
    
    void renameLocation(UUID playerUUID, String name, String newName, CommandSender sender) {
        if (playerData.containsKey(playerUUID)) {
            YamlConfiguration playerConfig = playerData.get(playerUUID);
            ConfigurationSection locationsSection = playerConfig.getConfigurationSection("locations");
            if (locationsSection != null && locationsSection.contains(name)) {
                String location = locationsSection.getString(name);
                locationsSection.set(name, null);
                locationsSection.set(newName, location);
                savePlayerData(playerUUID);
                sender.sendMessage("Location '" + name + "' has been successfully renamed to '" + newName + "'.");
            } else {
                sender.sendMessage("Location '" + name + "' does not exist.");
            }
        }
    }
}