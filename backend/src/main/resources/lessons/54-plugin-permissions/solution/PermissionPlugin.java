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
        if (!sender.hasPermission("byte.announce")) {
            sender.sendMessage("Недостаточно прав.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Использование: /announce <текст>");
            return true;
        }
        getServer().broadcastMessage("[Объявление] " + String.join(" ", args));
        return true;
    }
}
