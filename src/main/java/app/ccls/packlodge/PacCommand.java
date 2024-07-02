package app.ccls.packlodge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class PacCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public PacCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.plugin.getCommand("pac").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isPacCommandEnabled()) {
            sender.sendMessage("The /pac command is currently disabled.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("pac")) {
            if (!sender.hasPermission("pssu.pac")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("-print")) {
                handlePacPrintCommand(sender);
                return true;
            } else {
                sendPacLink(sender);
                return true;
            }
        }

        return false;
    }

    private void sendPacLink(CommandSender sender) {
        String pacLink = "https://5q12.ccls.icu/packlodge/pac/";

        ComponentBuilder message = new ComponentBuilder("Click Here To Access The PAC")
                .color(net.md_5.bungee.api.ChatColor.GREEN)
                .bold(true)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, pacLink))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to access the PAC")));

        sender.spigot().sendMessage(message.create());
    }

    private void handlePacPrintCommand(CommandSender sender) {
        new Thread(() -> {
            try {
                URL url = new URL("https://5q12.ccls.icu/packlodge/pac/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                InputStream in = connection.getInputStream();
                File webServerDir = new File(plugin.getDataFolder().getParentFile(), "packlodge-system/web-server");
                File outputFile = new File(webServerDir, "pac.html");

                try (FileOutputStream out = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                String baseUrl = plugin.getConfig().getString("web-server-url", "http://localhost");
                int accessPort = plugin.getConfig().getInt("web-server-access-port", plugin.getConfig().getInt("web-server-port", 8798));
                String urlLink = baseUrl + ":" + accessPort + "/web-server/pac.html";

                ComponentBuilder message = new ComponentBuilder("Click Here To View The PAC")
                        .color(net.md_5.bungee.api.ChatColor.GREEN)
                        .bold(true)
                        .event(new ClickEvent(ClickEvent.Action.OPEN_URL, urlLink))
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to view the PAC on the server")));

                sender.spigot().sendMessage(message.create());

            } catch (Exception e) {
                sender.sendMessage("Failed to download and save the PAC. Please try again later.");
                e.printStackTrace();
            }
        }).start();
    }

    private boolean isPacCommandEnabled() {
        return plugin.getConfig().getBoolean("pac-command", true);
    }

    public void enablePacCommand() {
        plugin.getConfig().set("pac-command", true);
        plugin.saveConfig();
    }

    public void disablePacCommand() {
        plugin.getConfig().set("pac-command", false);
        plugin.saveConfig();
    }
}
