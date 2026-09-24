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
        // TODO: если config.yml нет — скопируйте его из ресурсов
        //       и выведите «Создан config.yml из настроек по умолчанию».

        // TODO: дополните конфиг недостающими ключами из loadDefaults().
        Map<String, Object> config = new Yaml().load(Files.readString(FILE));
        System.out.println("Конфиг в порядке");

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
