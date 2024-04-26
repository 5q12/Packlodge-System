package app.ccls.packlodge;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
                    // location command implementation...
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
                        // Execute location listing asynchronously
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
                        // Execute location deletion asynchronously
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
                }
            }
            sender.sendMessage("Usage: /psd modpack <enable|disable> or /psd location <player> [save <location-name>|rename <old-name> <new-name>|list|del <location-name>]");
            return true;
        }
        return false;
    }
}
