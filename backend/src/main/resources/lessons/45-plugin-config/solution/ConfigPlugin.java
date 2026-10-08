import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigPlugin extends JavaPlugin implements Listener, CommandExecutor {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("info").setExecutor(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String prefix = getConfig().getString("prefix", "[Server]");
        String welcome = getConfig().getString("welcome", "Добро пожаловать, %player%!");
        String message = welcome.replace("%player%", event.getPlayer().getName());
        event.getPlayer().sendMessage(prefix + " " + message);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = getConfig().getString("prefix", "[Server]");
        for (String line : getConfig().getStringList("motd")) {
            sender.sendMessage(prefix + " " + line);
        }
        return true;
    }
}
