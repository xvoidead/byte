import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class Main {
    static final Path FILE = Path.of("config.yml");

    public static void main(String[] args) throws Exception {
        if (!Files.exists(FILE)) {
            try (InputStream in = Main.class.getResourceAsStream("/config.yml")) {
                Files.copy(in, FILE);
            }
            System.out.println("Создан config.yml из настроек по умолчанию");
        }

        Map<String, Object> loaded = new Yaml().load(Files.readString(FILE));
        Map<String, Object> config = loaded == null ? new LinkedHashMap<>() : new LinkedHashMap<>(loaded);

        List<String> added = new ArrayList<>();
        for (Map.Entry<String, Object> entry : loadDefaults().entrySet()) {
            if (!config.containsKey(entry.getKey())) {
                config.put(entry.getKey(), entry.getValue());
                added.add(entry.getKey());
            }
        }

        if (!added.isEmpty()) {
            save(config);
            System.out.println("Добавлены ключи: " + String.join(", ", added));
        } else {
            System.out.println("Конфиг в порядке");
        }

        System.out.println("Сервер " + config.get("server-name") + ", мест: " + config.get("max-players"));
    }

    /** Настройки по умолчанию из resources/config.yml. */
    static Map<String, Object> loadDefaults() throws Exception {
        try (InputStream in = Main.class.getResourceAsStream("/config.yml")) {
            return new Yaml().load(in);
        }
    }

    /** Записывает конфиг в файл: каждый ключ на своей строке. */
    static void save(Map<String, Object> config) throws Exception {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Files.writeString(FILE, new Yaml(options).dump(config));
    }
}
