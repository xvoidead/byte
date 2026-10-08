import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CountdownPlugin extends JavaPlugin implements CommandExecutor {
    @Override
    public void onEnable() {
        getCommand("countdown").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            sender.sendMessage("Укажите число от 1 до 10.");
            return true;
        }

        final int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Укажите число от 1 до 10.");
            return true;
        }
        if (seconds < 1 || seconds > 10) {
            sender.sendMessage("Укажите число от 1 до 10.");
            return true;
        }

        new BukkitRunnable() {
            private int remaining = seconds;

            @Override
            public void run() {
                if (remaining == 0) {
                    player.sendMessage("Старт!");
                    cancel();
                    return;
                }
                player.sendMessage("До старта: " + remaining);
                remaining--;
            }
        }.runTaskTimer(this, 1L, 20L);
        return true;
    }
}
