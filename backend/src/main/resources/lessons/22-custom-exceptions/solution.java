import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

class BankException extends RuntimeException {
    BankException(String message) {
        super(message);
    }
}

class AccountNotFoundException extends BankException {
    AccountNotFoundException(String name) {
        super("счёт " + name + " не найден");
    }
}

class InsufficientFundsException extends BankException {
    InsufficientFundsException(String name) {
        super("недостаточно средств на счёте " + name);
    }
}

class Bank {
    private final Map<String, Integer> accounts = new HashMap<>();

    void open(String name) {
        if (accounts.containsKey(name)) {
            throw new BankException("счёт " + name + " уже существует");
        }
        accounts.put(name, 0);
    }

    void deposit(String name, int amount) {
        checkAmount(amount);
        accounts.put(name, find(name) + amount);
    }

    void withdraw(String name, int amount) {
        checkAmount(amount);
        int balance = find(name);
        if (balance < amount) {
            throw new InsufficientFundsException(name);
        }
        accounts.put(name, balance - amount);
    }

    void transfer(String from, String to, int amount) {
        checkAmount(amount);
        find(from);
        find(to);
        withdraw(from, amount);
        deposit(to, amount);
    }

    int balance(String name) {
        return find(name);
    }

    private int find(String name) {
        Integer balance = accounts.get(name);
        if (balance == null) {
            throw new AccountNotFoundException(name);
        }
        return balance;
    }

    private static void checkAmount(int amount) {
        if (amount <= 0) {
            throw new BankException("сумма должна быть положительной");
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        Bank bank = new Bank();
        while (in.hasNext()) {
            String command = in.next();
            try {
                switch (command) {
                    case "open" -> bank.open(in.next());
                    case "deposit" -> bank.deposit(in.next(), in.nextInt());
                    case "withdraw" -> bank.withdraw(in.next(), in.nextInt());
                    case "transfer" -> bank.transfer(in.next(), in.next(), in.nextInt());
                    case "balance" -> {
                        String name = in.next();
                        int balance = bank.balance(name);
                        System.out.println(name + ": " + balance);
                    }
                    default -> System.out.println("Неизвестная команда: " + command);
                }
            } catch (BankException e) {
                System.out.println("Ошибка: " + e.getMessage());
            }
        }
    }
}
