import java.sql.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:game.db")) {
            try (Statement statement = db.createStatement()) {
                statement.execute("DROP TABLE IF EXISTS results");
                statement.execute("CREATE TABLE results (name TEXT, score INTEGER)");
            }

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
        try (Statement statement = db.createStatement()) {
            int games;
            try (ResultSet row = statement.executeQuery("SELECT COUNT(*), COUNT(DISTINCT name) FROM results")) {
                row.next();
                games = row.getInt(1);
                System.out.println("Игр: " + games);
                System.out.println("Игроков: " + row.getInt(2));
            }
            if (games == 0) {
                System.out.println("Рекордов пока нет");
                return;
            }

            try (ResultSet record = statement.executeQuery(
                    "SELECT name, score FROM results ORDER BY score DESC, name LIMIT 1")) {
                record.next();
                System.out.println("Рекорд: " + record.getInt("score") + " (" + record.getString("name") + ")");
            }

            System.out.println("Лучшие игроки:");
            try (ResultSet top = statement.executeQuery("""
                    SELECT name, MAX(score) AS best
                    FROM results
                    GROUP BY name
                    ORDER BY best DESC, name
                    LIMIT 3
                    """)) {
                int place = 1;
                while (top.next()) {
                    System.out.println(place + ". " + top.getString("name") + " — " + top.getInt("best"));
                    place++;
                }
            }
        }
    }
}
