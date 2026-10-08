import java.util.HashSet;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class WelcomePlugin extends JavaPlugin implements Listener {
    private final Set<String> seenPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);
        Player player = event.getPlayer();
        player.sendMessage("Добро пожаловать, " + player.getName() + "!");
        boolean firstTime = seenPlayers.add(player.getName().toLowerCase(java.util.Locale.ROOT));
        player.sendMessage(firstTime ? "Это ваш первый вход." : "С возвращением.");
        player.sendMessage("Сейчас на сервере: " + getServer().getOnlinePlayers().size());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }
}
