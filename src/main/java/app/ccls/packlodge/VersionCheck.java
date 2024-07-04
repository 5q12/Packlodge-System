package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class VersionCheck {

    private static final String VERSION_URL = "https://5q12.ccls.icu/packlodge/version.txt";
    private static final String CURRENT_VERSION = "1.2.0";
    private final JavaPlugin plugin;
    private boolean isOutdated = false;

    public VersionCheck(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(VERSION_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = in.readLine();
                in.close();

                if (latestVersion != null && !latestVersion.equals(CURRENT_VERSION)) {
                    isOutdated = true;
                    String message = "Packlodge-System is out of date. Run /psget update to get the latest version.";
                    plugin.getLogger().warning(message);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.broadcast(message, "minecraft.op");
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to check for updates: " + e.getMessage());
            }
        });
    }

    public boolean isOutdated() {
        return isOutdated;
    }
}

