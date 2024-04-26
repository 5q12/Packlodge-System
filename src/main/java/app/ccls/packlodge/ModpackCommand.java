package app.ccls.packlodge;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

public class ModpackCommand implements CommandExecutor {
    
    private final JavaPlugin plugin;

    public ModpackCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Check if the command is enabled
        if (!isModpackCommandEnabled()) {
            sender.sendMessage("The /modpack command is currently disabled.");
            return true; // Return here if the command is disabled
        }
        
        if (command.getName().equalsIgnoreCase("modpack")) {
            // Check for permission
            if (!sender.hasPermission("pssu.modpack")) {
                sender.sendMessage("You don't have permission to use this command.");
                return true;
            }
            
            FileConfiguration config = plugin.getConfig();
            String modpackLink = config.getString("modpack-link", "modpack.example.net");
            
            ComponentBuilder message = new ComponentBuilder("Click Here To Download The Latest Modpack")
                .color(net.md_5.bungee.api.ChatColor.GREEN)
                .bold(true)
                .event(new ClickEvent(ClickEvent.Action.OPEN_URL, modpackLink))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to download the modpack")));
                
            sender.spigot().sendMessage(message.create());
            
            return true;
        }
        
        return false;
    }

    // Method to check if the /modpack command is enabled
    private boolean isModpackCommandEnabled() {
        return plugin.getConfig().getBoolean("modpack-command", true);
    }

    // Method to enable the /modpack command
    public void enableModpackCommand() {
        plugin.getConfig().set("modpack-command", true);
        plugin.saveConfig();
    }

    // Method to disable the /modpack command
    public void disableModpackCommand() {
        plugin.getConfig().set("modpack-command", false);
        plugin.saveConfig();
    }
}
