import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyPlugin extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<String, Integer> balances = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("balance").setExecutor(this);
        getCommand("pay").setExecutor(this);
        getCommand("baltop").setExecutor(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        balances.putIfAbsent(event.getPlayer().getName(), 0);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        int reward = getConfig().getInt("rewards." + event.getBlock().getType().name(), 0);
        if (reward <= 0) {
            return;
        }
        Player player = event.getPlayer();
        balances.merge(player.getName(), reward, Integer::sum);
        player.sendMessage("+" + reward + " монет. Баланс: " + balance(player.getName()));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "balance" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Команда доступна игроку.");
                    return true;
                }
                sender.sendMessage("Баланс: " + balance(player.getName()) + " монет.");
            }
            case "pay" -> pay(sender, args);
            case "baltop" -> baltop(sender);
            default -> sender.sendMessage("Неизвестная команда.");
        }
        return true;
    }

    private void pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player from) || args.length != 2) {
            sender.sendMessage("Использование: /pay <игрок> <сумма>");
            return;
        }
        final int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("Сумма должна быть числом.");
            return;
        }
        if (amount <= 0) {
            sender.sendMessage("Сумма должна быть больше нуля.");
            return;
        }
        Player target = getServer().getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("Игрок " + args[0] + " не найден.");
            return;
        }
        int current = balance(from.getName());
        if (current < amount) {
            sender.sendMessage("Недостаточно монет.");
            return;
        }
        balances.put(from.getName(), current - amount);
        balances.merge(target.getName(), amount, Integer::sum);
        from.sendMessage("Переведено " + target.getName() + ": " + amount + " монет.");
        target.sendMessage("Получено от " + from.getName() + ": " + amount + " монет.");
    }

    private void baltop(CommandSender sender) {
        List<Player> players = new ArrayList<>(getServer().getOnlinePlayers());
        players.sort(Comparator
                .comparingInt((Player player) -> balance(player.getName())).reversed()
                .thenComparing(Player::getName));
        if (players.isEmpty()) {
            sender.sendMessage("Игроков онлайн нет.");
            return;
        }
        int limit = Math.min(5, players.size());
        for (int i = 0; i < limit; i++) {
            Player player = players.get(i);
            sender.sendMessage((i + 1) + ". " + player.getName() + " — " + balance(player.getName()));
        }
    }

    private int balance(String name) {
        return balances.getOrDefault(name, 0);
    }
}
