import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Main {
    public static void main(String[] args) throws Exception {
        Path file = Path.of("config.yml");
        if (!Files.exists(file)) {
            System.out.println("Нет файла config.yml");
            return;
        }

        Map<String, Object> config = new Yaml().load(Files.readString(file));

        String name = (String) config.get("server-name");
        int maxPlayers = (Integer) config.get("max-players");
        boolean pvp = (Boolean) config.get("pvp");
        Map<String, Object> spawn = (Map<String, Object>) config.get("spawn");
        List<String> motd = (List<String>) config.getOrDefault("motd", List.of());

        System.out.println("Сервер: " + name);
        System.out.println("Игроков: до " + maxPlayers);
        System.out.println("PvP: " + (pvp ? "включён" : "выключен"));
        System.out.println("Спавн: " + spawn.get("x") + ", " + spawn.get("y"));
        System.out.println("Сообщения: " + motd.size());
        for (String line : motd) {
            System.out.println("- " + line);
        }
    }
}
