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
        // TODO: прочитайте welcome и prefix из конфига.
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: выведите все строки motd с префиксом.
        return true;
    }
}
