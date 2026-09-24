package dev.byteide.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Библиотеки для уроков о базах данных и плагинах: всё работает внутри программы, без сети. */
class LibrariesRunTest {

    private static final RunnerProperties PROPERTIES = RunnerProperties.defaults();
    private static final ProcessExecutionService EXECUTOR =
            new ProcessExecutionService(PROPERTIES, new Sandbox(PROPERTIES));
    private final CodeRunner runner = new CodeRunner(CodeRunnerTest.COMPILER, EXECUTOR,
            new SandboxHealth(CodeRunnerTest.COMPILER, EXECUTOR), PROPERTIES);

    private static Project project(String... namesAndContents) {
        List<ProjectFile> files = new java.util.ArrayList<>();
        for (int i = 0; i < namesAndContents.length; i += 2) {
            files.add(new ProjectFile(namesAndContents[i], namesAndContents[i + 1]));
        }
        return new Project(files);
    }

    @Test
    void sqliteKeepsDataBetweenConnectionsAndWritesDatabaseFile() {
        RunResult result = runner.run(project("Main.java", """
                import java.sql.*;

                public class Main {
                    static Connection open() throws SQLException {
                        return DriverManager.getConnection("jdbc:sqlite:shop.db");
                    }

                    public static void main(String[] args) throws Exception {
                        try (Connection c = open(); Statement s = c.createStatement()) {
                            s.execute("CREATE TABLE items (id INTEGER PRIMARY KEY, name TEXT, price REAL)");
                        }
                        try (Connection c = open();
                             PreparedStatement ps = c.prepareStatement("INSERT INTO items (name, price) VALUES (?, ?)")) {
                            ps.setString(1, "Меч");
                            ps.setDouble(2, 9.5);
                            ps.executeUpdate();
                        }
                        try (Connection c = open(); Statement s = c.createStatement();
                             ResultSet r = s.executeQuery("SELECT id, name, price FROM items")) {
                            while (r.next()) {
                                System.out.println(r.getInt("id") + " " + r.getString("name") + " " + r.getDouble("price"));
                            }
                        }
                    }
                }
                """), "");

        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).isEqualTo("1 Меч 9.5\n");
        assertThat(result.files()).anySatisfy(f -> {
            assertThat(f.name()).isEqualTo("shop.db");
            assertThat(f.size()).isGreaterThan(0);
        });
    }

    @Test
    void mongoClientTalksToServerInsideProgram() {
        RunResult result = runner.run(project("Main.java", """
                import com.mongodb.client.*;
                import com.mongodb.client.model.*;
                import org.bson.Document;
                import java.util.List;

                public class Main {
                    public static void main(String[] args) {
                        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
                            MongoCollection<Document> players = client.getDatabase("game").getCollection("players");
                            players.insertMany(List.of(
                                    new Document("name", "Steve").append("level", 5),
                                    new Document("name", "Alex").append("level", 12)));
                            players.updateOne(Filters.eq("name", "Steve"), Updates.inc("level", 1));
                            for (Document d : players.find().sort(Sorts.descending("level"))) {
                                System.out.println(d.getString("name") + " " + d.getInteger("level"));
                            }
                        }
                    }
                }
                """), "");

        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).isEqualTo("Alex 12\nSteve 6\n");
    }

    @Test
    void postgresRunsInsideProgramWithMoreMemory() {
        RunResult result = runner.run(project("Main.java", """
                import java.sql.*;

                public class Main {
                    public static void main(String[] args) throws Exception {
                        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/shop", "shop", "secret");
                             Statement s = c.createStatement()) {
                            s.execute("CREATE TABLE items (id SERIAL PRIMARY KEY, name TEXT NOT NULL, tags TEXT[], data JSONB)");
                            try (PreparedStatement ps = c.prepareStatement(
                                    "INSERT INTO items (name, tags, data) VALUES (?, ?, ?::jsonb) RETURNING id")) {
                                ps.setString(1, "Меч");
                                ps.setArray(2, c.createArrayOf("text", new String[] {"оружие"}));
                                ps.setString(3, "{\\"damage\\": 7}");
                                try (ResultSet r = ps.executeQuery()) {
                                    r.next();
                                    System.out.println("id " + r.getInt(1));
                                }
                            }
                            try (ResultSet r = s.executeQuery("SELECT name, tags[1], data->>'damage' FROM items")) {
                                r.next();
                                System.out.println(r.getString(1) + " " + r.getString(2) + " " + r.getString(3));
                            }
                        }
                        // Второе соединение видит те же данные: база живёт, пока работает программа.
                        try (Connection c = DriverManager.getConnection("jdbc:postgresql://localhost/shop");
                             ResultSet r = c.createStatement().executeQuery("SELECT count(*) FROM items")) {
                            r.next();
                            System.out.println("всего " + r.getInt(1));
                        }
                    }
                }
                """), "");

        assertThat(result.stderr()).isEmpty();
        assertThat(result.stdout()).isEqualTo("id 1\nМеч оружие 7\nвсего 1\n");
    }

    @Test
    void postgresIsOnlyAvailableOnLocalhost() {
        RunResult result = runner.run(project("Main.java", """
                public class Main {
                    public static void main(String[] args) throws Exception {
                        java.sql.DriverManager.getConnection("jdbc:postgresql://10.0.0.1:5432/shop");
                    }
                }
                """), "");

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("PostgreSQL доступен только на localhost");
    }

    private static final String PLUGIN_YML = """
            name: Hello
            version: 1.0
            main: HelloPlugin
            api-version: "1.21"
            commands:
              heal:
                description: Лечит игрока
            """;

    @Test
    void mockBukkitLoadsPluginWithEventsAndCommands() {
        RunResult result = runner.run(project(
                "HelloPlugin.java", """
                        import org.bukkit.command.*;
                        import org.bukkit.event.*;
                        import org.bukkit.event.player.PlayerJoinEvent;
                        import org.bukkit.plugin.java.JavaPlugin;

                        public class HelloPlugin extends JavaPlugin implements Listener {
                            @Override
                            public void onEnable() {
                                getServer().getPluginManager().registerEvents(this, this);
                                getLogger().info("Плагин включён");
                            }

                            @EventHandler
                            public void onJoin(PlayerJoinEvent event) {
                                event.getPlayer().sendMessage("Привет, " + event.getPlayer().getName() + "!");
                            }

                            @Override
                            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                                sender.sendMessage("Здоровье восстановлено");
                                return true;
                            }
                        }
                        """,
                "Main.java", """
                        import org.mockbukkit.mockbukkit.MockBukkit;
                        import org.mockbukkit.mockbukkit.ServerMock;
                        import org.mockbukkit.mockbukkit.entity.PlayerMock;

                        public class Main {
                            public static void main(String[] args) {
                                ServerMock server = MockBukkit.mock();
                                MockBukkit.load(HelloPlugin.class);
                                PlayerMock steve = server.addPlayer("Steve");
                                System.out.println(steve.nextMessage());
                                steve.performCommand("heal");
                                System.out.println(steve.nextMessage());
                                MockBukkit.unmock();
                            }
                        }
                        """,
                "resources/plugin.yml", PLUGIN_YML), "");

        assertThat(result.status()).as(result.stderr()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Привет, Steve!\nЗдоровье восстановлено\n");
        // Журнал MockBukkit — без времени, чтобы вывод не менялся от запуска к запуску.
        assertThat(result.stderr()).isEqualTo("[INFO] Плагин включён\n");
    }

    @Test
    void pluginCodeKeepsStudentPermissionsWhileMockBukkitLoadsIt() {
        RunResult result = runner.run(project(
                "HelloPlugin.java", """
                        import org.bukkit.plugin.java.JavaPlugin;

                        public class HelloPlugin extends JavaPlugin {
                            @Override
                            public void onEnable() {
                                try {
                                    new java.net.URLClassLoader(new java.net.URL[0]);
                                    System.out.println("OPEN");
                                } catch (SecurityException e) {
                                    System.out.println("BLOCKED");
                                }
                            }
                        }
                        """,
                "Main.java", """
                        public class Main {
                            public static void main(String[] args) {
                                org.mockbukkit.mockbukkit.MockBukkit.mock();
                                org.mockbukkit.mockbukkit.MockBukkit.load(HelloPlugin.class);
                            }
                        }
                        """,
                "resources/plugin.yml", PLUGIN_YML), "");

        assertThat(result.stdout()).isEqualTo("BLOCKED\n");
    }
}
