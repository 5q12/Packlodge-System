package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TabListPing implements Listener {
    private final JavaPlugin plugin;

    public TabListPing(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startUpdatingPing();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updatePlayerPing(player);
    }

    private void startUpdatingPing() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayerPing(player);
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Update every second (20 ticks)
    }

    private void updatePlayerPing(Player player) {
        int ping = getPing(player);
        String pingMessage = "§a[" + ping + "ms]";
        player.setPlayerListName(player.getName() + " " + pingMessage);
    }

    private int getPing(Player player) {
        try {
            return player.getPing();
        } catch (NoSuchMethodError e) {
            try {
                Object handle = player.getClass().getMethod("getHandle").invoke(player);
                return (int) handle.getClass().getField("ping").get(handle);
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        }
    }
}
