import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class Main {
    public static void main(String[] args) throws Exception {
        Path file = Path.of("config.yml");
        // TODO: если файла нет — выведите «Нет файла config.yml» и завершите программу.

        // TODO: прочитайте файл и разберите его: new Yaml().load(...)
        Map<String, Object> config = Map.of();

        System.out.println("Сервер: " + config.get("server-name"));
        // TODO: игроки, PvP, точка спавна и сообщения
    }
}
