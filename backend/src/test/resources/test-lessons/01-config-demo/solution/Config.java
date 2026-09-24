import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public record Config(String greeting, int maxPlayers) {
    private static final Path FILE = Path.of("config.yml");

    public static Config load() throws Exception {
        Map<String, Object> defaults;
        try (InputStream in = Config.class.getResourceAsStream("/config.yml")) {
            defaults = new Yaml().load(in);
        }
        Map<String, Object> values = new LinkedHashMap<>(defaults);
        boolean changed = !Files.exists(FILE);
        if (!changed) {
            Map<String, Object> saved = new Yaml().load(Files.readString(FILE));
            for (String key : defaults.keySet()) {
                if (saved.containsKey(key)) {
                    values.put(key, saved.get(key));
                } else {
                    changed = true;
                }
            }
        }
        if (changed) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Files.writeString(FILE, new Yaml(options).dump(values));
        }
        return new Config((String) values.get("greeting"), (Integer) values.get("max-players"));
    }
}
