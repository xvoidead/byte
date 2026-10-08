import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class PermissionPlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        getCommand("announce").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: проверьте byte.announce, затем соберите и разошлите объявление.
        return true;
    }
}
