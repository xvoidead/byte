import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public record ServerConfig(String serverName, int maxPlayers, Difficulty difficulty, int spawnProtection) {

    public static ServerConfig load(Path file) throws Exception {
        // TODO: нет файла, сломанный YAML или не словарь — ConfigException с одной ошибкой.
        Map<String, Object> map = new Yaml().load(Files.readString(file));
        return from(map);
    }

    static ServerConfig from(Map<String, Object> map) {
        // TODO: проверьте каждое значение и соберите ошибки в список,
        //       а если он не пуст — бросьте new ConfigException(errors).
        String serverName = (String) map.get("server-name");
        int maxPlayers = (Integer) map.get("max-players");
        Difficulty difficulty = Difficulty.valueOf(((String) map.get("difficulty")).toUpperCase());
        int spawnProtection = (Integer) map.get("spawn-protection");
        return new ServerConfig(serverName, maxPlayers, difficulty, spawnProtection);
    }
}
