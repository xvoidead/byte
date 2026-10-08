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
        online.add(event.getPlayer().getUniqueId());
        event.setJoinMessage(null);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) {
        online.remove(event.getPlayer().getUniqueId());
        event.setQuitMessage(null);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("Игроков онлайн: " + online.size());
        return true;
    }
}
