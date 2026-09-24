package dev.byteide.runner;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Проект ученика — один или несколько файлов. Старые клиенты и уроки присылают один исходник,
 * он становится проектом из {@code Main.java}.
 */
public record Project(List<ProjectFile> files) {

    public static final String MAIN_FILE = "Main.java";
    static final String RESOURCES = "resources/";
    public static final int MAX_FILES = 30;

    /** Части пути: латиница, цифры, «_», «-», «.»; не начинаются с точки; не глубже трёх уровней. */
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}(/[A-Za-z0-9_][A-Za-z0-9_.-]{0,63}){0,2}");

    public Project {
        files = List.copyOf(files);
    }

    public static Project single(String source) {
        return new Project(List.of(new ProjectFile(MAIN_FILE, source)));
    }

    public List<ProjectFile> sources() {
        return files.stream().filter(ProjectFile::isSource).toList();
    }

    public List<ProjectFile> resources() {
        return files.stream().filter(ProjectFile::isResource).toList();
    }

    public List<ProjectFile> workFiles() {
        return files.stream().filter(ProjectFile::isWorkFile).toList();
    }

    public int totalLength() {
        return files.stream().mapToInt(f -> f.content().length()).sum();
    }

    /**
     * Программа подключается к PostgreSQL. В песочнице это PGlite — настоящий PostgreSQL в WebAssembly внутри
     * самой программы, и ему нужно гораздо больше памяти, чем обычной программе. Адрес подключения может лежать
     * и в конфиге, поэтому смотрим все файлы проекта.
     */
    public boolean usesPostgres() {
        return files.stream().anyMatch(f -> f.content().contains("jdbc:postgresql") || f.content().contains("org.postgresql"));
    }

    /** Весь исходный код одной строкой — для поиска по нему (например, требований к решению). */
    public String allSources() {
        StringBuilder sb = new StringBuilder();
        for (ProjectFile file : sources()) {
            sb.append(file.content()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Проверяет состав проекта и возвращает понятную ученику ошибку или null.
     *
     * @param maxLength максимальная суммарная длина файлов
     */
    public String problem(int maxLength) {
        if (files.isEmpty() || sources().isEmpty()) {
            return "В проекте нет ни одного файла .java.";
        }
        if (files.size() > MAX_FILES) {
            return "Слишком много файлов: максимум " + MAX_FILES + ".";
        }
        Set<String> names = new HashSet<>();
        for (ProjectFile file : files) {
            String name = file.name();
            if (name == null || !NAME.matcher(name).matches() || name.contains("..")) {
                return "Недопустимое имя файла «" + name + "». Используйте латиницу, цифры, «_», «-» и «.», "
                        + "папки через «/».";
            }
            if (!names.add(name.toLowerCase())) {
                return "Два файла с одинаковым именем: " + name + ".";
            }
        }
        if (sources().stream().allMatch(f -> f.content().isBlank())) {
            return "Код программы пуст.";
        }
        if (totalLength() > maxLength) {
            return "Проект слишком большой: максимум " + maxLength + " символов во всех файлах.";
        }
        return null;
    }
}
