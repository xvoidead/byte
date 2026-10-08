import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class MessagePlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        getCommand("msg").setExecutor(this);
        getCommand("broadcast").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // TODO: обработайте msg и broadcast, проверьте аргументы и права.
        return true;
    }
}
