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
        // TODO: нет файла — «Нет файла server.properties».
        Properties props = load();

        // TODO: узнайте версию (config-version, по умолчанию 1), проверьте её
        //       и примените миграции по очереди: 1 → 2, 2 → 3.

        boolean pvp = Boolean.parseBoolean(props.getProperty("pvp"));
        System.out.println("Сервер: " + props.getProperty("server-name")
                + ", мест: " + props.getProperty("max-players")
                + ", PvP: " + (pvp ? "да" : "нет")
                + ", дальность: " + props.getProperty("view-distance"));
    }

    /** Читает server.properties. */
    static Properties load() throws Exception {
        Properties props = new Properties();
        // TODO: прочитайте файл в кодировке UTF-8: Files.newBufferedReader(FILE)
        return props;
    }

    /** Версия 1 → 2: name переименован в server-name, slots — в max-players. */
    static void migrateTo2(Properties props) {
        // TODO
    }

    /** Версия 2 → 3: pvp из yes/no становится true/false, появился view-distance=10. */
    static void migrateTo3(Properties props) {
        // TODO
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
