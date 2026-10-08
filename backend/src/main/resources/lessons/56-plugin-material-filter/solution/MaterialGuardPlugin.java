import java.util.Set;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class MaterialGuardPlugin extends JavaPlugin implements Listener {
    private final Set<Material> allowed = Set.of(Material.STONE, Material.DIAMOND_ORE);
    @Override public void onEnable() { getServer().getPluginManager().registerEvents(this, this); }
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!allowed.contains(event.getBlock().getType())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Этот блок нельзя добывать.");
        }
    }
}
