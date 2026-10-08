import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class OnlineRosterPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final Set<UUID> online = new HashSet<>();
    @Override public void onEnable() { getServer().getPluginManager().registerEvents(this, this); getCommand("online").setExecutor(this); }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        // TODO: добавьте UUID игрока.
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        // TODO: удалите UUID и скройте стандартное сообщение.
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: покажите число UUID в Set.
        return true;
    }
}
