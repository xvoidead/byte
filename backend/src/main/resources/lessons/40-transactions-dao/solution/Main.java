import java.sql.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws SQLException {
        try (Connection db = DriverManager.getConnection("jdbc:sqlite:bank.db")) {
            AccountDao dao = new AccountDao(db);
            dao.createTables();
            Scanner in = new Scanner(System.in);
            while (in.hasNextLine()) {
                String[] parts = in.nextLine().strip().split("\\s+");
                switch (parts[0]) {
                    case "open" -> {
                        int balance = Integer.parseInt(parts[2]);
                        if (dao.open(parts[1], balance)) {
                            System.out.println("Открыт счёт " + parts[1] + ": " + balance);
                        } else {
                            System.out.println("Счёт " + parts[1] + " уже есть");
                        }
                    }
                    case "transfer" -> {
                        int amount = Integer.parseInt(parts[3]);
                        try {
                            dao.transfer(parts[1], parts[2], amount);
                            System.out.println("Перевод " + amount + ": " + parts[1] + " → " + parts[2]);
                        } catch (TransferException e) {
                            System.out.println("Ошибка: " + e.getMessage());
                        }
                    }
                    case "balance" -> {
                        for (Account account : dao.all()) {
                            System.out.println(account.name() + ": " + account.balance());
                        }
                    }
                    case "history" -> {
                        var history = dao.history();
                        if (history.isEmpty()) {
                            System.out.println("Переводов нет");
                        }
                        for (Transfer t : history) {
                            System.out.println("#" + t.id() + " " + t.from() + " → " + t.to() + ": " + t.amount());
                        }
                    }
                    default -> { }
                }
            }
        }
    }
}
