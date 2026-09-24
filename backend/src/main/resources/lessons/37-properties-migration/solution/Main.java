import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

public class Main {
    static final Path FILE = Path.of("server.properties");
    static final int CURRENT_VERSION = 3;

    public static void main(String[] args) throws Exception {
        if (!Files.exists(FILE)) {
            System.out.println("Нет файла server.properties");
            return;
        }
        Properties props = load();

        int version;
        try {
            version = Integer.parseInt(props.getProperty("config-version", "1").strip());
        } catch (NumberFormatException e) {
            version = 0;
        }
        if (version < 1) {
            System.out.println("config-version должен быть целым числом больше 0");
            return;
        }
        if (version > CURRENT_VERSION) {
            System.out.println("Версия конфига " + version + " новее программы (" + CURRENT_VERSION + ")");
            return;
        }

        if (version == CURRENT_VERSION) {
            System.out.println("Конфиг актуален (версия " + CURRENT_VERSION + ")");
        } else {
            while (version < CURRENT_VERSION) {
                System.out.println("Миграция " + version + " → " + (version + 1));
                switch (version) {
                    case 1 -> migrateTo2(props);
                    case 2 -> migrateTo3(props);
                    default -> throw new IllegalStateException("Нет миграции с версии " + version);
                }
                version++;
            }
            props.setProperty("config-version", String.valueOf(CURRENT_VERSION));
            save(props);
            System.out.println("Конфиг обновлён до версии " + CURRENT_VERSION);
        }

        boolean pvp = Boolean.parseBoolean(props.getProperty("pvp"));
        System.out.println("Сервер: " + props.getProperty("server-name")
                + ", мест: " + props.getProperty("max-players")
                + ", PvP: " + (pvp ? "да" : "нет")
                + ", дальность: " + props.getProperty("view-distance"));
    }

    /** Читает server.properties. */
    static Properties load() throws Exception {
        Properties props = new Properties();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            props.load(reader);
        }
        return props;
    }

    /** Версия 1 → 2: name переименован в server-name, slots — в max-players. */
    static void migrateTo2(Properties props) {
        rename(props, "name", "server-name");
        rename(props, "slots", "max-players");
    }

    /** Версия 2 → 3: pvp из yes/no становится true/false, появился view-distance=10. */
    static void migrateTo3(Properties props) {
        boolean pvp = props.getProperty("pvp", "no").equalsIgnoreCase("yes");
        props.setProperty("pvp", String.valueOf(pvp));
        if (props.getProperty("view-distance") == null) {
            props.setProperty("view-distance", "10");
        }
    }

    static void rename(Properties props, String from, String to) {
        String value = (String) props.remove(from);
        if (value != null) {
            props.setProperty(to, value);
        }
    }

    /** Записывает настройки по строке «ключ=значение», ключи по алфавиту. */
    static void save(Properties props) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String key : new TreeSet<>(props.stringPropertyNames())) {
            lines.add(key + "=" + props.getProperty(key));
        }
        Files.write(FILE, lines);
    }
}
