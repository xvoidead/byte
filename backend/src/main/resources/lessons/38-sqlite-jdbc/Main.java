import java.sql.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:game.db")) {
            try (Statement statement = db.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS results");
                statement.execute("CREATE TABLE results (name TEXT, score INTEGER)");
            }

            // Сохраняем результаты партий. PreparedStatement подробно разберём в следующем уроке.
            Scanner in = new Scanner(System.in);
            try (PreparedStatement insert = db.prepareStatement("INSERT INTO results (name, score) VALUES (?, ?)")) {
                while (in.hasNext()) {
                    insert.setString(1, in.next());
                    insert.setInt(2, in.nextInt());
                    insert.executeUpdate();
                }
            }

            printReport(db);
        }
    }

    static void printReport(Connection db) throws SQLException {
        // TODO: посчитайте и выведите отчёт SQL-запросами
    }
}
