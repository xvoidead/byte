package dev.byteide.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/** Проекты из нескольких файлов, рабочая папка программы и библиотеки для учеников. */
class ProjectRunTest {

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

    private static String main(String body) {
        return """
                import java.io.*;
                import java.nio.file.*;
                import java.util.*;

                public class Main {
                    public static void main(String[] args) throws Exception {
                        %s
                    }
                }
                """.formatted(body);
    }

    @Test
    void compilesSeveralFilesAndRunsMain() {
        RunResult result = runner.run(project(
                "Main.java", "public class Main { public static void main(String[] a) { System.out.println(new Greeter().hi(\"Ада\")); } }",
                "Greeter.java", "public class Greeter { String hi(String n) { return \"Привет, \" + n; } }"), "");

        assertThat(result.status()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Привет, Ада\n");
    }

    @Test
    void reportsErrorsWithFileNames() {
        RunResult result = runner.run(project(
                "Main.java", "public class Main { public static void main(String[] a) { new Greeter().hi(); } }",
                "Greeter.java", "public class Greeter {\n    void hi() { int x = \"текст\"; }\n}"), "");

        assertThat(result.status()).isEqualTo(RunStatus.COMPILATION_ERROR);
        assertThat(result.diagnostics()).anySatisfy(d -> {
            assertThat(d.file()).isEqualTo("Greeter.java");
            assertThat(d.line()).isEqualTo(2);
        });
    }

    @Test
    void publicClassMustMatchFileNameInMultiFileProject() {
        RunResult result = runner.run(project(
                "Main.java", "public class Main { public static void main(String[] a) { } }",
                "Util.java", "public class Helper { }"), "");

        assertThat(result.status()).isEqualTo(RunStatus.COMPILATION_ERROR);
        assertThat(result.diagnostics()).anySatisfy(d -> assertThat(d.file()).isEqualTo("Util.java"));
    }

    @Test
    void readsAndWritesFilesInWorkDirAndReturnsThem() {
        RunResult result = runner.run(project(
                "Main.java", main("""
                        String name = Files.readString(Path.of("name.txt")).strip();
                        Files.createDirectories(Path.of("out"));
                        Files.writeString(Path.of("out/greeting.txt"), "Привет, " + name + "\\n");
                        try (var w = new FileWriter("log.txt")) { w.write("ok"); }
                        System.out.println(new File(".").list().length);
                        """),
                "name.txt", "Ада\n"), "");

        assertThat(result.status()).as(result.stderr()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("3\n");
        assertThat(result.files()).extracting(OutputFile::name)
                .containsExactly("log.txt", "name.txt", "out/greeting.txt");
        assertThat(result.files()).filteredOn(f -> f.name().equals("out/greeting.txt"))
                .singleElement().extracting(OutputFile::content).isEqualTo("Привет, Ада\n");
    }

    @Test
    void binaryFilesAreReturnedWithoutContent() {
        RunResult result = runner.run(project("Main.java", main("Files.write(Path.of(\"data.bin\"), new byte[]{0, 1, 2, (byte) 0xff});")), "");

        assertThat(result.files()).singleElement().satisfies(f -> {
            assertThat(f.content()).isNull();
            assertThat(f.size()).isEqualTo(4);
        });
    }

    @Test
    void resourcesAreOnClasspath() {
        RunResult result = runner.run(project(
                "Main.java", main("""
                        try (InputStream in = Main.class.getResourceAsStream("/config.yml")) {
                            System.out.print(new String(in.readAllBytes()));
                        }
                        """),
                "resources/config.yml", "max-players: 20\n"), "");

        assertThat(result.status()).as(result.stderr()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("max-players: 20\n");
        assertThat(result.files()).isEmpty();
    }

    @Test
    void snakeYamlAndGsonAreAvailable() {
        RunResult result = runner.run(project(
                "Main.java", """
                        import com.google.gson.*;
                        import java.nio.file.*;
                        import java.util.*;
                        import org.yaml.snakeyaml.Yaml;

                        public class Main {
                            record Settings(String motd, int maxPlayers, List<String> admins) {}

                            public static void main(String[] args) throws Exception {
                                Map<String, Object> yaml = new Yaml().load(Files.readString(Path.of("config.yml")));
                                System.out.println(yaml.get("motd") + " " + yaml.get("max-players"));
                                Settings s = new Gson().fromJson(Files.readString(Path.of("settings.json")), Settings.class);
                                System.out.println(s);
                                Files.writeString(Path.of("out.json"), new GsonBuilder().setPrettyPrinting().create().toJson(s));
                                List<Settings> list = new Gson().fromJson("[" + Files.readString(Path.of("settings.json")) + "]",
                                        new com.google.gson.reflect.TypeToken<List<Settings>>() {}.getType());
                                System.out.println(list.size());
                                System.out.print(new Yaml().dump(Map.of("ok", true)));
                            }
                        }
                        """,
                "config.yml", "motd: Привет\nmax-players: 20\n",
                "settings.json", "{\"motd\": \"hi\", \"maxPlayers\": 5, \"admins\": [\"ada\"]}"), "");

        assertThat(result.status()).as(result.stderr()).isEqualTo(RunStatus.SUCCESS);
        assertThat(result.stdout()).isEqualTo("Привет 20\nSettings[motd=hi, maxPlayers=5, admins=[ada]]\n1\n{ok: true}\n");
        assertThat(result.files()).filteredOn(f -> f.name().equals("out.json")).singleElement()
                .extracting(OutputFile::content).asString().contains("\"maxPlayers\": 5");
    }

    @Test
    void limitsSizeOfSingleFile() {
        RunResult result = runner.run(project("Main.java", main("""
                byte[] mb = new byte[1 << 20];
                try (var out = Files.newOutputStream(Path.of("big.bin"))) {
                    for (int i = 0; i < 10; i++) out.write(mb);
                }
                """)), "");

        assertThat(result.status()).isEqualTo(RunStatus.RUNTIME_ERROR);
        assertThat(result.stderr()).contains("File too large");
    }

    @Test
    void limitsTotalSizeOfWorkDir() {
        RunResult result = runner.run(project("Main.java", main("""
                byte[] mb = new byte[1 << 20];
                for (int i = 0; i < 20; i++) Files.write(Path.of("part" + i + ".bin"), mb);
                Thread.sleep(3000);
                """)), "");

        assertThat(result.status()).isEqualTo(RunStatus.FILES_LIMIT);
    }

    @Test
    void limitsNumberOfFiles() {
        RunResult result = runner.run(project("Main.java", main("""
                for (int i = 0; i < 1000; i++) Files.writeString(Path.of("f" + i + ".txt"), "x");
                Thread.sleep(3000);
                """)), "");

        assertThat(result.status()).isEqualTo(RunStatus.FILES_LIMIT);
    }

    @Test
    void workDirIsFreshForEveryRun() {
        Project project = project("Main.java", main("""
                System.out.println(Files.exists(Path.of("mark.txt")));
                Files.writeString(Path.of("mark.txt"), "x");
                """));

        assertThat(runner.run(project, "").stdout()).isEqualTo("false\n");
        assertThat(runner.run(project, "").stdout()).isEqualTo("false\n");
    }

    @Test
    void validatesProjectFiles() {
        assertThat(project("Main.java", "class Main {}").problem(1000)).isNull();
        assertThat(project("../evil.java", "x").problem(1000)).contains("Недопустимое имя");
        assertThat(project("/abs.java", "x").problem(1000)).contains("Недопустимое имя");
        assertThat(project(".hidden", "x", "Main.java", "class Main {}").problem(1000)).contains("Недопустимое имя");
        assertThat(project("a/b/c/d.txt", "x", "Main.java", "class Main {}").problem(1000)).contains("Недопустимое имя");
        assertThat(project("config.yml", "x").problem(1000)).contains("нет ни одного файла .java");
        assertThat(project("Main.java", "class Main {}", "main.java", "class M {}").problem(1000)).contains("одинаковым");
        assertThat(project("Main.java", "x".repeat(2000)).problem(1000)).contains("слишком большой");
    }
}
