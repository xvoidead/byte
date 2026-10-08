import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.simulate.entity.PlayerSimulation;

public class Main {
    public static void main(String[] args) {
        ServerMock server = MockBukkit.mock();
        Map<String, PlayerMock> players = new LinkedHashMap<>();
        try {
            MockBukkit.load(BlockQuestPlugin.class);
            Scanner input = new Scanner(System.in);
            while (input.hasNextLine()) {
                String line = input.nextLine().trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s+", 2);
                String rest = parts.length == 2 ? parts[1] : "";
                switch (parts[0]) {
                    case "join" -> {
                        PlayerMock player = server.addPlayer(rest);
                        players.put(rest, player);
                    }
                    case "quit" -> {
                        PlayerMock player = players.get(rest);
                        if (player != null) {
                            player.disconnect();
                        }
                    }
                    case "op", "deop" -> {
                        PlayerMock player = players.get(rest);
                        if (player != null) {
                            player.setOp(parts[0].equals("op"));
                        }
                    }
                    case "cmd" -> {
                        String[] command = rest.split("\\s+", 2);
                        PlayerMock player = command.length == 2 ? players.get(command[0]) : null;
                        if (player != null) {
                            player.performCommand(command[1]);
                        }
                    }
                    case "chat" -> {
                        String[] chat = rest.split("\\s+", 2);
                        PlayerMock player = chat.length == 2 ? players.get(chat[0]) : null;
                        if (player != null) {
                            player.chat(chat[1]);
                        }
                    }
                    case "break" -> simulateBreak(server, players, rest);
                    case "place" -> simulatePlace(players, rest);
                    case "wait" -> server.getScheduler().performTicks(Long.parseLong(rest));
                    default -> throw new IllegalArgumentException("Неизвестная команда сценария: " + parts[0]);
                }
                flush(players.values());
            }
        } finally {
            MockBukkit.unmock();
        }
    }

    private static void simulateBreak(ServerMock server, Map<String, PlayerMock> players, String rest) {
        String[] args = rest.split("\\s+");
        if (args.length != 5) {
            return;
        }
        PlayerMock player = players.get(args[0]);
        if (player == null) {
            return;
        }
        Block block = server.getWorld("world").getBlockAt(
                Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        block.setType(Material.valueOf(args[4].toUpperCase(Locale.ROOT)));
        new PlayerSimulation(player).simulateBlockBreak(block);
    }

    private static void simulatePlace(Map<String, PlayerMock> players, String rest) {
        String[] args = rest.split("\\s+");
        if (args.length != 5) {
            return;
        }
        PlayerMock player = players.get(args[0]);
        if (player == null) {
            return;
        }
        Location location = new Location(player.getWorld(),
                Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        new PlayerSimulation(player).simulateBlockPlace(
                Material.valueOf(args[4].toUpperCase(Locale.ROOT)), location);
    }

    private static void flush(Collection<PlayerMock> players) {
        for (Player player : players) {
            Component message;
            while ((message = ((PlayerMock) player).nextComponentMessage()) != null) {
                String text = PlainTextComponentSerializer.plainText().serialize(message);
                System.out.println("[" + player.getName() + "] " + text);
            }
        }
    }
}
