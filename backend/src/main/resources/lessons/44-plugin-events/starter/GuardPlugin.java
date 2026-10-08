import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.plugin.java.JavaPlugin;

public class GuardPlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        // TODO: отмените ломание в радиусе 5, кроме операторов.
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        // TODO: отмените установку в радиусе 5, кроме операторов.
    }
}
