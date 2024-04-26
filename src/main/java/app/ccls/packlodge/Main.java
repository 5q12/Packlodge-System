package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;


public final class Main extends JavaPlugin {

    private FileConfiguration playerDataConfig;
    private File playerDataFile;
    private HashMap<UUID, Long> playTimeMap;
    private LocationCommands locationCommands; // Add this field
    private ModpackCommand modpackCommand;

    @Override
    public void onEnable() {
        // Plugin startup logic
        System.out.println("Packlodge System Loaded Successfully");
        saveDefaultConfig();

        // permission-and-commands.yml creation and deletion process logic
        File file = new File(getDataFolder(), "permissions-and-commands.yml");
        if (file.exists()) file.delete();
        saveResource("permissions-and-commands.yml", false);
        getLogger().info("permissions-and-commands.yml created or overwritten successfully.");
        
        ModpackCommand modpackCommand = new ModpackCommand(this);
        // Create plugin folder if it doesn't exist
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

        // Initialize player data file and configuration
        playerDataFile = new File(pluginFolder, "players.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        playerDataConfig = getConfig();
        playTimeMap = new HashMap<>();

        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        // Register the /location command
        locationCommands = new LocationCommands(this); // Initialize LocationCommands
        getCommand("location").setExecutor(locationCommands);
        // Register the /seen command
        getCommand("seen").setExecutor(new SeenCommand());
        // Register the /playtime command
        getCommand("playtime").setExecutor(new PlayTime(this));
        // Register the /playtime command
        getCommand("modpack").setExecutor(new ModpackCommand(this));
        // Registering the PSDCommand command executor
        getCommand("psd").setExecutor(new PSDCommand(this, modpackCommand));

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        System.out.println("Packlodge System Successfully Shutdown");
    
    }

    // Add this method to provide access to LocationCommands instance
    public LocationCommands getLocationCommands() {
        return locationCommands;
    
    }
    private class SeenCommand implements org.bukkit.command.CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            // Check if the sender has the required permission
            if (!sender.hasPermission("pssu.seen")) {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }

            // Check if the command has the correct number of arguments
            if (args.length != 1) {
                sender.sendMessage("Usage: /seen <username>");
                return true;
            }

            // Concatenate arguments into username in case username contains spaces
            String targetUsername = String.join(" ", args);

            // Check if the player is online
            Player targetPlayer = Bukkit.getPlayer(targetUsername);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                sender.sendMessage("Player: " + targetUsername + " is currently online.");
                return true;
            }

            // Find player in the data file
            ConfigurationSection playersSection = playerDataConfig.getConfigurationSection("players");
            if (playersSection == null) {
                sender.sendMessage("No player data found.");
                return true;
            }

            for (String key : playersSection.getKeys(false)) {
                if (playersSection.getString(key + ".username").equalsIgnoreCase(targetUsername)) {
                    long lastOnline = playersSection.getLong(key + ".lastOnline");
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd yyyy HH:mm:ss (z)");
                    sdf.setTimeZone(TimeZone.getTimeZone("Europe/Stockholm"));
                    String lastOnlineString = sdf.format(new Date(lastOnline));
                    sender.sendMessage("Player: " + targetUsername + " was last online at: " + lastOnlineString);
                    return true;
                }
            }

            sender.sendMessage("Player: " + targetUsername + " could not be found.");
            return true;
        }
    }
    private class PlayerListener implements Listener {
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();
            String playerName = player.getName();
            long currentTimeMillis = System.currentTimeMillis();
            Location playerLocation = player.getLocation();

            // Get player's IP address
            String ip = getIpAddress(player);

            // Initialize playtime for the player
            playTimeMap.put(playerUUID, 0L);

            // Update player data
            updatePlayerData(playerUUID, playerName, currentTimeMillis, ip, playerLocation);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();

            // Save playtime to file when player quits
            savePlayTime(playerUUID);
        }

        private void savePlayTime(UUID playerUUID) {
            ConfigurationSection playerSection = playerDataConfig.createSection("players." + playerUUID.toString() + ".playtime");
            long playTime = playTimeMap.getOrDefault(playerUUID, 0L);
            playerSection.set("seconds", playTime);
            saveConfig();
        }


        private String getIpAddress(Player player) {
            String ip = player.getAddress().getAddress().getHostAddress();
            return ip != null ? ip : "Unknown";
        }

        private void updatePlayerData(UUID playerUUID, String playerName, long lastOnline, String ip, Location location) {
            String key = playerUUID.toString();
            ConfigurationSection playerSection = playerDataConfig.createSection("players." + key);
            playerSection.set("username", playerName);
            playerSection.set("lastOnline", lastOnline);
            playerSection.set("ip", ip);
            if (location != null) {
                playerSection.set("coordinates.x", location.getX());
                playerSection.set("coordinates.y", location.getY());
                playerSection.set("coordinates.z", location.getZ());
                playerSection.set("coordinates.world", location.getWorld().getName());
            }
            savePlayerDataConfig();
        }
    }
    private void savePlayerDataConfig() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

