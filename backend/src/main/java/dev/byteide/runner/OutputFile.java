package dev.byteide.runner;

/**
 * Файл из рабочей папки программы после её завершения.
 *
 * @param content текст файла или null, если файл двоичный или слишком большой, чтобы его показывать
 * @param size    размер в байтах
 */
public record OutputFile(String name, String content, long size) {
}
