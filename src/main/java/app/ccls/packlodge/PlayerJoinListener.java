package app.ccls.packlodge;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;

public class PlayerJoinListener implements Listener {
    @SuppressWarnings("unused")
    private final JavaPlugin plugin;
    private final VersionCheck versionCheck;

    public PlayerJoinListener(JavaPlugin plugin, VersionCheck versionCheck) {
        this.plugin = plugin;
        this.versionCheck = versionCheck;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (versionCheck.isOutdated() && player.isOp()) {
            String message = ChatColor.RED + "WARNING " + ChatColor.RESET + ChatColor.GREEN + "" + ChatColor.BOLD + "Packlodge-System Version (Stable) is out of date. Run " + ChatColor.RESET + ChatColor.GOLD + "" + ChatColor.ITALIC + "/psget update" + ChatColor.RESET + ChatColor.GREEN + "" + ChatColor.BOLD + " to get the latest (Stable) version.";
            player.sendMessage(message);
        }
    }
}

