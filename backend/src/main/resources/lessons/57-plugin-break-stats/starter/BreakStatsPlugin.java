import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BreakStatsPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<UUID, Integer> blocks = new HashMap<>();
    @Override public void onEnable() { getServer().getPluginManager().registerEvents(this, this); getCommand("blocks").setExecutor(this); }
    @EventHandler public void onJoin(PlayerJoinEvent event) { blocks.putIfAbsent(event.getPlayer().getUniqueId(), 0); }
    @EventHandler public void onBreak(BlockBreakEvent event) {
        // TODO: считайте только STONE.
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Команда доступна игроку."); return true; }
        // TODO: покажите счёт игрока.
        return true;
    }
}
