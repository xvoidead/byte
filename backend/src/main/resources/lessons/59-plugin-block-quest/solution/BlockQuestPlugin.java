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
import org.bukkit.plugin.java.JavaPlugin;

public class BlockQuestPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<UUID, Integer> progress = new HashMap<>();
    private static final int GOAL = 3;
    @Override public void onEnable() { getServer().getPluginManager().registerEvents(this, this); getCommand("quest").setExecutor(this); }
    @EventHandler public void onBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() != Material.STONE) return;
        int count = progress.merge(event.getPlayer().getUniqueId(), 1, Integer::sum);
        if (count == GOAL) event.getPlayer().sendMessage("Квест выполнен: 3/3");
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Квест доступен игроку."); return true; }
        int count = Math.min(progress.getOrDefault(player.getUniqueId(), 0), GOAL);
        player.sendMessage("Прогресс: " + count + "/3");
        return true;
    }
}
