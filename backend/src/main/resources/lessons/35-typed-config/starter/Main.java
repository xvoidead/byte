import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws Exception {
        try {
            ServerConfig config = ServerConfig.load(Path.of("config.yml"));
            System.out.println("Сервер: " + config.serverName());
            System.out.println("Игроков: до " + config.maxPlayers());
            System.out.println("Сложность: " + config.difficulty().name().toLowerCase());
            System.out.println("Защита спавна: " + config.spawnProtection());
        } catch (ConfigException e) {
            System.out.println("Ошибки в config.yml:");
            for (String error : e.errors()) {
                System.out.println("- " + error);
            }
        }
    }
}
