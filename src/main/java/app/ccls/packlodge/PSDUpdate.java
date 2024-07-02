package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PSDUpdate implements CommandExecutor {

    private final JavaPlugin plugin;

    public PSDUpdate(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getCommand("psget").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("pssu.dev.get")) {
            sender.sendMessage("§cYou don't have permission to execute this command.");
            return true;
        }

        if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("-beta"))) {
            if (args[0].equalsIgnoreCase("update")) {
                boolean isBeta = args.length == 2 && args[1].equalsIgnoreCase("-beta");
                updatePlugin(sender, isBeta);
                return true;
            } else if (args[0].equalsIgnoreCase("upgrade")) {
                upgradePlugin(sender);
                return true;
            }
        }
        return false;
    }

    private void updatePlugin(CommandSender sender, boolean isBeta) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String urlString = isBeta 
                            ? "https://5q12.ccls.icu/packlodge/plugin/downloads/beta/packlodge-system-latest.jar" 
                            : "https://5q12.ccls.icu/packlodge/plugin/downloads/packlodge-system-latest.jar";
                    URL url = new URL(urlString);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setDoOutput(true);
                    InputStream in = connection.getInputStream();

                    File pluginFolder = plugin.getDataFolder().getParentFile();
                    File outputFile = new File(pluginFolder, "packlodge-system-latest.jar");

                    try (FileOutputStream out = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                    sender.sendMessage("Successfully downloaded the latest " + (isBeta ? "beta " : "") + "packlodge-system version. To confirm update do /psget upgrade. (NOTE: this will stop the server you will need to manually start it again)");
                } catch (Exception e) {
                    sender.sendMessage("Failed to update " + (isBeta ? "beta " : "") + "packlodge-system. Please try again later.");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void upgradePlugin(CommandSender sender) {
        sender.sendMessage("Server will shut down for upgrade...");

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String scriptName = System.getProperty("os.name").toLowerCase().contains("win") ? "upgrade.bat" : "upgrade.sh";
                    File scriptFile = new File(plugin.getDataFolder().getParentFile().getParentFile(), "scripts/" + scriptName);
                    if (scriptFile.exists()) {
                        ProcessBuilder pb = new ProcessBuilder(scriptFile.getAbsolutePath());
                        pb.inheritIO();
                        pb.start();
                    } else {
                        sender.sendMessage("Upgrade script not found: " + scriptName);
                    }
                } catch (IOException e) {
                    sender.sendMessage("Failed to execute upgrade script.");
                    e.printStackTrace();
                } finally {
                    Bukkit.getServer().shutdown();
                }
            }
        }.runTask(plugin);
    }
}
