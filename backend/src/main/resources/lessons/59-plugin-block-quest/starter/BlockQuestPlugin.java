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
        // TODO: учитывайте только STONE и сообщите при достижении цели.
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Квест доступен игроку."); return true; }
        // TODO: покажите прогресс игрока.
        return true;
    }
}
