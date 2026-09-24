public record Config(String greeting, int maxPlayers) {
    public static Config load() {
        return new Config("Привет", 20); // TODO: прочитайте config.yml
    }
}
