import java.util.Arrays;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MessagePlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        getCommand("msg").setExecutor(this);
        getCommand("broadcast").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("msg")) {
            if (args.length < 2) {
                sender.sendMessage("Использование: /msg <игрок> <текст>");
                return true;
            }
            Player target = getServer().getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage("Игрок " + args[0] + " не найден.");
                return true;
            }
            String text = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            target.sendMessage("От " + sender.getName() + ": " + text);
            sender.sendMessage("Кому " + target.getName() + ": " + text);
            return true;
        }

        if (!sender.isOp()) {
            sender.sendMessage("Только операторы могут объявлять.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Использование: /broadcast <текст>");
            return true;
        }
        String text = String.join(" ", args);
        getServer().broadcastMessage("[Объявление] " + text);
        return true;
    }
}
