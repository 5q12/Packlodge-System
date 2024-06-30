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
import java.io.InputStream;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;

public final class Main extends JavaPlugin {

    private HTTPServer httpServer;
    private FileConfiguration playerDataConfig;
    private File playerDataFile;
    private HashMap<UUID, Long> playTimeMap;
    private LocationCommands locationCommands;
    private ModpackCommand modpackCommand;

    @Override
    public void onEnable() {
        System.out.println("Packlodge System Loaded Successfully");
        this.getCommand("psget").setExecutor(new PSDUpdate(this));
        saveDefaultConfig();

        File file = new File(getDataFolder(), "permissions-and-commands.yml");
        if (file.exists()) file.delete();
        saveResource("permissions-and-commands.yml", false);
        getLogger().info("permissions-and-commands.yml created or overwritten successfully.");

        modpackCommand = new ModpackCommand(this);
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }

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

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        locationCommands = new LocationCommands(this);
        getCommand("location").setExecutor(locationCommands);
        getCommand("seen").setExecutor(new SeenCommand());
        getCommand("playtime").setExecutor(new PlayTime(this));
        getCommand("modpack").setExecutor(modpackCommand);
        getCommand("psd").setExecutor(new PSDCommand(this, modpackCommand));

        copyUpgradeScript();

       if (getConfig().getBoolean("web-server", false)) {
            try {
                httpServer = new HTTPServer(this);
                httpServer.startServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisable() {
        System.out.println("Packlodge System Successfully Shutdown");
        if (httpServer != null) {
            httpServer.stopServer();
        }
    }

    public LocationCommands getLocationCommands() {
        return locationCommands;
    }
    private void copyUpgradeScript() {
        File serverHomeDir = getDataFolder().getParentFile().getParentFile();
        File scriptFolder = new File(serverHomeDir, "scripts");
        if (!scriptFolder.exists()) {
            scriptFolder.mkdirs();
        }
    
        String scriptName = System.getProperty("os.name").toLowerCase().contains("win") ? "upgrade.bat" : "upgrade.sh";
        File scriptFile = new File(scriptFolder, scriptName);
    
        if (!scriptFile.exists()) {
            try (InputStream in = getResource(scriptName)) {
                if (in != null) {
                    Files.copy(in, scriptFile.toPath());
                    scriptFile.setExecutable(true);
                } else {
                    getLogger().severe("Script resource not found: " + scriptName);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }    

    private class SeenCommand implements org.bukkit.command.CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("pssu.seen")) {
                sender.sendMessage("§cYou don't have permission to execute this command.");
                return true;
            }

            if (args.length != 1) {
                sender.sendMessage("Usage: /seen <username>");
                return true;
            }

            String targetUsername = String.join(" ", args);

            Player targetPlayer = Bukkit.getPlayer(targetUsername);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                sender.sendMessage("Player: " + targetUsername + " is currently online.");
                return true;
            }

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

            String ip = getIpAddress(player);

            playTimeMap.put(playerUUID, 0L);

            updatePlayerData(playerUUID, playerName, currentTimeMillis, ip, playerLocation);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();

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
