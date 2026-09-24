import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public record ServerConfig(String serverName, int maxPlayers, Difficulty difficulty, int spawnProtection) {

    public static ServerConfig load(Path file) throws Exception {
        if (!Files.exists(file)) {
            throw new ConfigException(List.of("файл не найден"));
        }
        Object data;
        try {
            data = new Yaml().load(Files.readString(file));
        } catch (YAMLException e) {
            throw new ConfigException(List.of("файл не читается как YAML"));
        }
        if (data == null) {
            return from(Map.of());
        }
        if (!(data instanceof Map)) {
            throw new ConfigException(List.of("файл не читается как YAML"));
        }
        return from((Map<String, Object>) data);
    }

    static ServerConfig from(Map<String, Object> map) {
        List<String> errors = new ArrayList<>();

        Object nameValue = map.get("server-name");
        String serverName = nameValue == null ? "" : nameValue.toString().strip();
        if (serverName.isEmpty()) {
            errors.add("server-name не задан");
        }

        Integer maxPlayers = integer(map, "max-players", null, errors);
        if (maxPlayers != null && maxPlayers <= 0) {
            errors.add("max-players должен быть больше 0");
        }

        Difficulty difficulty = null;
        Object difficultyValue = map.get("difficulty");
        if (difficultyValue == null) {
            errors.add("difficulty не задан");
        } else {
            try {
                difficulty = Difficulty.valueOf(difficultyValue.toString().toUpperCase());
            } catch (IllegalArgumentException e) {
                errors.add("difficulty должен быть одним из: peaceful, easy, normal, hard");
            }
        }

        Integer spawnProtection = integer(map, "spawn-protection", 16, errors);
        if (spawnProtection != null && spawnProtection < 0) {
            errors.add("spawn-protection не может быть отрицательным");
        }

        if (!errors.isEmpty()) {
            throw new ConfigException(errors);
        }
        return new ServerConfig(serverName, maxPlayers, difficulty, spawnProtection);
    }

    /** Целое число из конфига; если ключа нет — значение по умолчанию (null — ключ обязателен). */
    private static Integer integer(Map<String, Object> map, String key, Integer defaultValue, List<String> errors) {
        Object value = map.get(key);
        if (value == null) {
            if (defaultValue == null) {
                errors.add(key + " не задан");
            }
            return defaultValue;
        }
        if (value instanceof Integer number) {
            return number;
        }
        errors.add(key + " должен быть целым числом");
        return null;
    }
}
