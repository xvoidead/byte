import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Всё, что связано с SQL: счета игроков и история переводов. */
public class AccountDao {
    private final Connection db;

    public AccountDao(Connection db) {
        this.db = db;
    }

    public void createTables() throws SQLException {
        try (Statement s = db.createStatement()) {
            s.execute("DROP TABLE IF EXISTS transfers");
            s.execute("DROP TABLE IF EXISTS accounts");
            s.execute("""
                    CREATE TABLE accounts (
                        name    TEXT PRIMARY KEY,
                        balance INTEGER NOT NULL CHECK (balance >= 0)
                    )""");
            s.execute("""
                    CREATE TABLE transfers (
                        id        INTEGER PRIMARY KEY,
                        from_name TEXT NOT NULL,
                        to_name   TEXT NOT NULL,
                        amount    INTEGER NOT NULL
                    )""");
        }
    }

    /** Открывает счёт; false, если счёт с таким именем уже есть. */
    public boolean open(String name, int balance) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement("INSERT OR IGNORE INTO accounts (name, balance) VALUES (?, ?)")) {
            insert.setString(1, name);
            insert.setInt(2, balance);
            return insert.executeUpdate() > 0;
        }
    }

    public List<Account> all() throws SQLException {
        List<Account> result = new ArrayList<>();
        try (PreparedStatement select = db.prepareStatement("SELECT name, balance FROM accounts ORDER BY name");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                result.add(new Account(rows.getString("name"), rows.getInt("balance")));
            }
        }
        return result;
    }

    public List<Transfer> history() throws SQLException {
        List<Transfer> result = new ArrayList<>();
        try (PreparedStatement select = db.prepareStatement("SELECT id, from_name, to_name, amount FROM transfers ORDER BY id");
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                result.add(new Transfer(rows.getInt("id"), rows.getString("from_name"), rows.getString("to_name"),
                        rows.getInt("amount")));
            }
        }
        return result;
    }

    // TODO: перевод работает без транзакции. Если начисление прошло, а списание — нет,
    // монеты появляются из ниоткуда. Проверки тоже идут не в том порядке.
    public void transfer(String from, String to, int amount) throws SQLException, TransferException {
        if (amount <= 0) {
            throw new TransferException("сумма должна быть больше нуля");
        }
        try (PreparedStatement credit = db.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE name = ?");
             PreparedStatement debit = db.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE name = ?");
             PreparedStatement log = db.prepareStatement("INSERT INTO transfers (from_name, to_name, amount) VALUES (?, ?, ?)")) {
            credit.setInt(1, amount);
            credit.setString(2, to);
            if (credit.executeUpdate() == 0) {
                throw new TransferException("нет счёта " + to);
            }
            debit.setInt(1, amount);
            debit.setString(2, from);
            try {
                if (debit.executeUpdate() == 0) {
                    throw new TransferException("нет счёта " + from);
                }
            } catch (SQLException e) {
                throw new TransferException("у " + from + " недостаточно монет");
            }
            log.setString(1, from);
            log.setString(2, to);
            log.setInt(3, amount);
            log.executeUpdate();
        }
    }
}
