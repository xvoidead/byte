import java.sql.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:shop.db")) {
            try (Statement statement = db.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS items");
                statement.execute("CREATE TABLE items (name TEXT PRIMARY KEY, price INTEGER NOT NULL)");
            }
            Scanner in = new Scanner(System.in);
            while (in.hasNextLine()) {
                String[] parts = in.nextLine().strip().split(" ", 2);
                String argument = parts.length > 1 ? parts[1] : "";
                switch (parts[0]) {
                    case "add" -> {
                        String[] priceAndName = argument.split(" ", 2);
                        add(db, priceAndName[1], Integer.parseInt(priceAndName[0]));
                    }
                    case "price" -> price(db, argument);
                    case "cheaper" -> cheaper(db, Integer.parseInt(argument));
                    case "remove" -> remove(db, argument);
                    case "count" -> count(db);
                    default -> { }
                }
            }
        }
    }

    // TODO: склейка строк ломается на апострофе и открывает дорогу SQL-инъекциям.
    // Перепишите все запросы на PreparedStatement с параметрами.

    static void add(Connection db, String name, int price) throws SQLException {
        try (Statement s = db.createStatement()) {
            ResultSet existing = s.executeQuery("SELECT COUNT(*) FROM items WHERE name = '" + name + "'");
            existing.next();
            if (existing.getInt(1) > 0) {
                System.out.println("Уже есть: " + name);
                return;
            }
            s.executeUpdate("INSERT INTO items (name, price) VALUES ('" + name + "', " + price + ")");
            System.out.println("Добавлен: " + name + " (" + price + ")");
        }
    }

    static void price(Connection db, String name) throws SQLException {
        try (Statement s = db.createStatement()) {
            ResultSet row = s.executeQuery("SELECT price FROM items WHERE name = '" + name + "'");
            if (row.next()) {
                System.out.println(name + ": " + row.getInt("price"));
            } else {
                System.out.println("Не найден: " + name);
            }
        }
    }

    static void cheaper(Connection db, int maxPrice) throws SQLException {
        try (Statement s = db.createStatement()) {
            ResultSet rows = s.executeQuery("SELECT name, price FROM items WHERE price <= " + maxPrice
                    + " ORDER BY price, name");
            boolean found = false;
            while (rows.next()) {
                System.out.println(rows.getString("name") + " — " + rows.getInt("price"));
                found = true;
            }
            if (!found) {
                System.out.println("Ничего не найдено");
            }
        }
    }

    static void remove(Connection db, String name) throws SQLException {
        try (Statement s = db.createStatement()) {
            if (s.executeUpdate("DELETE FROM items WHERE name = '" + name + "'") > 0) {
                System.out.println("Удалён: " + name);
            } else {
                System.out.println("Не найден: " + name);
            }
        }
    }

    static void count(Connection db) throws SQLException {
        try (Statement s = db.createStatement()) {
            ResultSet row = s.executeQuery("SELECT COUNT(*) FROM items");
            row.next();
            System.out.println("Товаров: " + row.getInt(1));
        }
    }
}
