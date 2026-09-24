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

    /** Переводит монеты в одной транзакции: при любой ошибке ничего не меняется. */
    public void transfer(String from, String to, int amount) throws SQLException, TransferException {
        if (amount <= 0) {
            throw new TransferException("сумма должна быть больше нуля");
        }
        if (from.equals(to)) {
            throw new TransferException("нельзя перевести самому себе");
        }
        db.setAutoCommit(false);
        try {
            withdraw(from, amount);
            deposit(to, amount);
            log(from, to, amount);
            db.commit();
        } catch (SQLException | TransferException e) {
            db.rollback();
            throw e;
        } finally {
            db.setAutoCommit(true);
        }
    }

    private void withdraw(String name, int amount) throws SQLException, TransferException {
        try (PreparedStatement debit = db.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE name = ?")) {
            debit.setInt(1, amount);
            debit.setString(2, name);
            int updated;
            try {
                updated = debit.executeUpdate();
            } catch (SQLException e) {
                // CHECK (balance >= 0) не дал уйти в минус
                throw new TransferException("у " + name + " недостаточно монет");
            }
            if (updated == 0) {
                throw new TransferException("нет счёта " + name);
            }
        }
    }

    private void deposit(String name, int amount) throws SQLException, TransferException {
        try (PreparedStatement credit = db.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE name = ?")) {
            credit.setInt(1, amount);
            credit.setString(2, name);
            if (credit.executeUpdate() == 0) {
                throw new TransferException("нет счёта " + name);
            }
        }
    }

    private void log(String from, String to, int amount) throws SQLException {
        try (PreparedStatement insert = db.prepareStatement(
                "INSERT INTO transfers (from_name, to_name, amount) VALUES (?, ?, ?)")) {
            insert.setString(1, from);
            insert.setString(2, to);
            insert.setInt(3, amount);
            insert.executeUpdate();
        }
    }
}
