import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CooldownPlugin extends JavaPlugin implements CommandExecutor {
    private final Set<UUID> coolingDown = new HashSet<>();
    @Override public void onEnable() { getCommand("gift").setExecutor(this); }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage("Команда доступна игроку."); return true; }
        UUID id = player.getUniqueId();
        if (!coolingDown.add(id)) { player.sendMessage("Подождите перед новым подарком."); return true; }
        player.sendMessage("Подарок получен!");
        new BukkitRunnable() {
            @Override public void run() { coolingDown.remove(id); }
        }.runTaskLater(this, 5L);
        return true;
    }
}
