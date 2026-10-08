import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class GuardPlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.getPlayer().isOp() || !isSpawn(event.getBlock().getLocation())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("У спавна нельзя ломать блоки.");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        if (event.getPlayer().isOp() || !isSpawn(event.getBlockPlaced().getLocation())) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage("У спавна нельзя ставить блоки.");
    }

    private boolean isSpawn(Location location) {
        double x = location.getX();
        double z = location.getZ();
        return x * x + z * z <= 25;
    }
}
