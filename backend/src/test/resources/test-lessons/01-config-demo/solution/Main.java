public class Main {
    public static void main(String[] args) throws Exception {
        Config config = Config.load();
        System.out.println(config.greeting() + "! Мест: " + config.maxPlayers());
    }
}
