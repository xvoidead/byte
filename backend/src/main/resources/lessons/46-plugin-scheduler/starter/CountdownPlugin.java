import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CountdownPlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        getCommand("countdown").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: проверьте N и запустите BukkitRunnable.
        return true;
    }
}
