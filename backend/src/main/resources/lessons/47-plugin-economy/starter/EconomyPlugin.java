import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyPlugin extends JavaPlugin implements Listener, CommandExecutor {
    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("balance").setExecutor(this);
        getCommand("pay").setExecutor(this);
        getCommand("baltop").setExecutor(this);
    }

    public void onJoin(PlayerJoinEvent event) {
        // TODO: добавьте баланс 0, если игрок новый.
    }

    public void onBreak(BlockBreakEvent event) {
        // TODO: прочитайте награду из конфига и начислите монеты.
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: реализуйте balance, pay и baltop.
        return true;
    }
}
