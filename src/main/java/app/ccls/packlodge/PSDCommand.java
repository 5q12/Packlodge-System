package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PSDCommand implements CommandExecutor {

    private final Main main;
    private final ModpackCommand modpackCommand;

    public PSDCommand(Main main, ModpackCommand modpackCommand) {
        this.main = main;
        this.modpackCommand = modpackCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pssu.dev")) {
            sender.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("psd")) {
            if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("location")) {
                    if (args.length == 4 && args[2].equalsIgnoreCase("save")) {
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer == null) {
                            sender.sendMessage("Player not found.");
                            return true;
                        }
                        main.getLocationCommands().saveLocation(targetPlayer.getUniqueId(), args[3], targetPlayer.getLocation());
                        sender.sendMessage("Saved current location as '" + args[3] + "' for player " + targetPlayer.getName());
                        return true;
                    } else if (args.length == 5 && args[2].equalsIgnoreCase("rename")) {
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer == null) {
                            sender.sendMessage("Player not found.");
                            return true;
                        }
                        main.getLocationCommands().renameLocation(targetPlayer.getUniqueId(), args[3], args[4], sender);
                        sender.sendMessage("Renamed location '" + args[3] + "' to '" + args[4] + "' for player " + targetPlayer.getName());
                        return true;
                    } else if (args.length == 3 && args[2].equalsIgnoreCase("list")) {
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer == null) {
                            sender.sendMessage("Player not found.");
                            return true;
                        }
                        main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                            main.getLocationCommands().listLocations(targetPlayer.getUniqueId(), sender);
                        });
                        return true;
                    } else if (args.length == 4 && args[2].equalsIgnoreCase("del")) {
                        Player targetPlayer = Bukkit.getPlayer(args[1]);
                        if (targetPlayer == null) {
                            sender.sendMessage("Player not found.");
                            return true;
                        }
                        main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                            main.getLocationCommands().deleteLocation(targetPlayer.getUniqueId(), args[3], sender);
                            sender.sendMessage("Deleted location '" + args[3] + "' for player " + targetPlayer.getName());
                        });
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("modpack")) {
                    if (args[1].equalsIgnoreCase("enable")) {
                        modpackCommand.enableModpackCommand();
                        sender.sendMessage("The /modpack command has been enabled.");
                        return true;
                    } else if (args[1].equalsIgnoreCase("disable")) {
                        modpackCommand.disableModpackCommand();
                        sender.sendMessage("The /modpack command has been disabled.");
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("web-server")) {
                    if (args.length == 2) {
                        handleWebServerCommands(sender, args);
                        return true;
                    } else if (args.length == 3) {
                        handleWebServerSettings(sender, args);
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("web-server-auth")) {
                    if (args.length == 2) {
                        handleWebServerAuthSetting(sender, args);
                        return true;
                    }
                } else if (args[0].equalsIgnoreCase("modpack-link") && args.length == 2) {
                    main.getConfig().set("modpack-link", args[1]);
                    main.saveConfig();
                    sender.sendMessage("Modpack download link updated to " + args[1]);
                    return true;
                }
            }

            if (args.length == 2) {
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }

                if (args[1].equalsIgnoreCase("locate")) {
                    sendPlayerLocation(sender, targetPlayer);
                    return true;
                } else if (args[1].equalsIgnoreCase("ip")) {
                    sendPlayerIP(sender, targetPlayer);
                    return true;
                } else if (args[1].equalsIgnoreCase("info")) {
                    sendPlayerInfo(sender, targetPlayer);
                    return true;
                }
            } else if (args.length == 3 && args[1].equalsIgnoreCase("info") && args[2].equalsIgnoreCase("-print")) {
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer == null) {
                    sender.sendMessage("Player not found.");
                    return true;
                }

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            File infoFile = generatePlayerInfoFile(targetPlayer);
                            String baseUrl = main.getConfig().getString("web-server-url", "http://localhost");
                            int accessPort = main.getConfig().getInt("web-server-access-port", main.getConfig().getInt("web-server-port", 8798));
                            String url = baseUrl + ":" + accessPort + "/web-server/prints/" + targetPlayer.getName() + ".txt";
                            ComponentBuilder message = new ComponentBuilder("Successfully generated player info file. [CLICK HERE]")
                                .color(net.md_5.bungee.api.ChatColor.GREEN)
                                .bold(true)
                                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to download the player info file")));
                            sender.spigot().sendMessage(message.create());
                        } catch (IOException e) {
                            sender.sendMessage("Failed to create player info file.");
                            e.printStackTrace();
                        }
                    }
                }.runTaskAsynchronously(main);
                return true;
            }

            sender.sendMessage("Usage: /psd modpack <enable|disable> or /psd location <player> [save <location-name>|rename <old-name> <new-name>|list|del <location-name>] or /psd <player> <locate|ip|info> or /psd <player> info -print or /psd modpack-link <link>");
            return true;
        }
        return false;
    }

    private void handleWebServerCommands(CommandSender sender, String[] args) {
        switch (args[1].toLowerCase()) {
            case "enable":
                main.getConfig().set("web-server", true);
                sender.sendMessage("Web server enabled.");
                break;
            case "disable":
                main.getConfig().set("web-server", false);
                sender.sendMessage("Web server disabled.");
                break;
            default:
                sender.sendMessage("Invalid web-server command.");
                break;
        }
        main.saveConfig();
    }

    private void handleWebServerSettings(CommandSender sender, String[] args) {
        switch (args[1].toLowerCase()) {
            case "url":
                main.getConfig().set("web-server-url", args[2]);
                sender.sendMessage("Web server URL updated to " + args[2]);
                break;
            case "access-port":
                main.getConfig().set("web-server-access-port", Integer.parseInt(args[2]));
                sender.sendMessage("Web server access port updated to " + args[2]);
                break;
            case "port":
                main.getConfig().set("web-server-port", Integer.parseInt(args[2]));
                sender.sendMessage("Web server port updated to " + args[2]);
                break;
            default:
                sender.sendMessage("Invalid web-server setting.");
                break;
        }
        main.saveConfig();
    }

    private void handleWebServerAuthSetting(CommandSender sender, String[] args) {
        if (args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
            boolean authSetting = Boolean.parseBoolean(args[1]);
            main.getConfig().set("web-server-authentication", authSetting);
            main.saveConfig();
            sender.sendMessage("Web server authentication set to " + authSetting);
        } else {
            sender.sendMessage("Invalid value for web-server-auth. Use true or false.");
        }
    }

    private void sendPlayerLocation(CommandSender sender, Player targetPlayer) {
        Location loc = targetPlayer.getLocation();
        String dimension = targetPlayer.getWorld().getEnvironment().toString();
        String coordinates = String.format("X: %.2f, Y: %.2f, Z: %.2f", loc.getX(), loc.getY(), loc.getZ());
        sender.sendMessage("Player " + targetPlayer.getName() + " is at " + coordinates + " in " + dimension);
    }

    private void sendPlayerIP(CommandSender sender, Player targetPlayer) {
        String ipAddress = targetPlayer.getAddress().getAddress().getHostAddress();
        sender.sendMessage("Player " + targetPlayer.getName() + "'s IP address is " + ipAddress);
    }

    private void sendPlayerInfo(CommandSender sender, Player targetPlayer) {
        sendPlayerLocation(sender, targetPlayer);
        sendPlayerIP(sender, targetPlayer);
    }

    private File generatePlayerInfoFile(Player targetPlayer) throws IOException {
        String ipAddress = targetPlayer.getAddress().getAddress().getHostAddress();
        String country = getCountryFromIP(ipAddress);
        String region = getRegionFromIP(ipAddress);
        Location loc = targetPlayer.getLocation();
        String dimension = targetPlayer.getWorld().getEnvironment().toString();

        File printsFolder = new File(main.getDataFolder().getParentFile(), "packlodge-system/web-server/prints");
        if (!printsFolder.exists()) {
            printsFolder.mkdirs();
        }

        File file = new File(printsFolder, targetPlayer.getName() + ".txt");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("player: " + targetPlayer.getName() + "\n");
            writer.write("ip: " + ipAddress + "\n");
            writer.write("country: " + country + "\n");
            writer.write("region: " + region + "\n");
            writer.write("coordinates:\n");
            writer.write("  x: " + loc.getX() + "\n");
            writer.write("  y: " + loc.getY() + "\n");
            writer.write("  z: " + loc.getZ() + "\n");
            writer.write("dimension: " + dimension + "\n");
        }
        return file;
    }

    private String getCountryFromIP(String ip) {
        try {
            JsonObject json = getIPInfo(ip);
            return json.get("country_name").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }

    private String getRegionFromIP(String ip) {
        try {
            JsonObject json = getIPInfo(ip);
            return json.get("region").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
    private JsonObject getIPInfo(String ip) throws IOException {
        String urlString = "https://ipapi.co/" + ip + "/json/";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
    
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to get data from ipapi: HTTP response code " + responseCode);
        }
    
        InputStreamReader reader = new InputStreamReader(conn.getInputStream());
        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
        reader.close();
        return json;
    }
}
