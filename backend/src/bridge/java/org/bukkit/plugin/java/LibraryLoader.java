package org.bukkit.plugin.java;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;

import org.bukkit.plugin.PluginDescriptionFile;

/*
 * Заменяет загрузчик библиотек плагинов из Paper API (лицензия GPL) в песочнице Byte. Оригинал скачивает
 * библиотеки из plugin.yml (ключ libraries) из Maven Central через Maven Resolver. В песочнице нет сети,
 * поэтому Maven Resolver не подключён, а без этой замены Paper при каждом запуске предупреждал бы,
 * что LibraryLoader не удалось создать.
 */
public class LibraryLoader {

    public static BiFunction<URL[], ClassLoader, URLClassLoader> LIBRARY_LOADER_FACTORY;
    public static Function<List<Path>, List<Path>> REMAPPER;

    public LibraryLoader(Logger logger) {
    }

    public ClassLoader createLoader(PluginDescriptionFile description) {
        return createLoader(description, List.of());
    }

    public ClassLoader createLoader(PluginDescriptionFile description, List<Path> paths) {
        if (!description.getLibraries().isEmpty()) {
            throw new UnsupportedOperationException("В песочнице нет сети: библиотеки из plugin.yml (libraries) "
                    + "не скачиваются. Уберите ключ libraries — Gson, SnakeYAML и Adventure уже доступны.");
        }
        return null;
    }
}
