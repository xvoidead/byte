package io.roastedroot.sqlite4j.core;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.Map;

import io.roastedroot.sqlite4j.SQLiteConfig;
import io.roastedroot.zerofs.Configuration;
import io.roastedroot.zerofs.ZeroFs;

/*
 * Изменено для песочницы Byte (sqlite4j распространяется по лицензии Apache 2.0). SQLite, скомпилированный
 * в WebAssembly, работает с копией файла базы в памяти. В оригинале эта память очищалась, когда закрывалось
 * последнее соединение, а на диск ничего не записывалось. Здесь, как у настоящего SQLite:
 * - база переживает закрытие соединений до конца программы;
 * - при закрытии соединения файл базы записывается на диск, в рабочую папку программы.
 */
public class WasmDBFactory {

    /** Одна файловая система на всю программу: её разделяют все соединения. */
    private static FileSystem fs;

    private final Map<WasmDB, String> files = new IdentityHashMap<>();

    public WasmDBFactory() {
    }

    public synchronized WasmDB create(String url, String fileName, SQLiteConfig config, boolean isMemory)
            throws SQLException {
        if (fs == null) {
            fs = ZeroFs.newFileSystem(Configuration.unix().toBuilder().setAttributeViews("unix").build());
        }
        WasmDB db = new WasmDB(fs, url, fileName, config, isMemory);
        if (!isMemory) {
            files.put(db, fileName);
        }
        return db;
    }

    public synchronized void close(WasmDB db) throws SQLException {
        db.close();
        String fileName = files.remove(db);
        if (fileName != null) {
            save(fileName);
        }
    }

    private static void save(String fileName) throws SQLException {
        Path inMemory = fs.getPath(fileName);
        if (!Files.exists(inMemory)) {
            return;
        }
        try {
            Files.copy(inMemory, Path.of(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new SQLException("Не удалось записать файл базы " + fileName + ": " + e.getMessage(), e);
        }
    }
}
