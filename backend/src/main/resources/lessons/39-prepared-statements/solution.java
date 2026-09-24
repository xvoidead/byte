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

    static void add(Connection db, String name, int price) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement("INSERT OR IGNORE INTO items (name, price) VALUES (?, ?)")) {
            insert.setString(1, name);
            insert.setInt(2, price);
            if (insert.executeUpdate() == 0) {
                System.out.println("Уже есть: " + name);
            } else {
                System.out.println("Добавлен: " + name + " (" + price + ")");
            }
        }
    }

    static void price(Connection db, String name) throws SQLException {
        try (PreparedStatement find = db.prepareStatement("SELECT price FROM items WHERE name = ?")) {
            find.setString(1, name);
            try (ResultSet row = find.executeQuery()) {
                if (row.next()) {
                    System.out.println(name + ": " + row.getInt("price"));
                } else {
                    System.out.println("Не найден: " + name);
                }
            }
        }
    }

    static void cheaper(Connection db, int maxPrice) throws SQLException {
        try (PreparedStatement find = db.prepareStatement(
                "SELECT name, price FROM items WHERE price <= ? ORDER BY price, name")) {
            find.setInt(1, maxPrice);
            try (ResultSet rows = find.executeQuery()) {
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
    }

    static void remove(Connection db, String name) throws SQLException {
        try (PreparedStatement delete = db.prepareStatement("DELETE FROM items WHERE name = ?")) {
            delete.setString(1, name);
            if (delete.executeUpdate() > 0) {
                System.out.println("Удалён: " + name);
            } else {
                System.out.println("Не найден: " + name);
            }
        }
    }

    static void count(Connection db) throws SQLException {
        try (PreparedStatement count = db.prepareStatement("SELECT COUNT(*) FROM items");
             ResultSet row = count.executeQuery()) {
            row.next();
            System.out.println("Товаров: " + row.getInt(1));
        }
    }
}
