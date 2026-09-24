import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

class BankException extends RuntimeException {
    BankException(String message) {
        super(message);
    }
}

// Создайте AccountNotFoundException и InsufficientFundsException

class Bank {
    private final Map<String, Integer> accounts = new HashMap<>();

    void open(String name) {
        accounts.put(name, 0);
    }

    void deposit(String name, int amount) {
    }

    void withdraw(String name, int amount) {
    }

    void transfer(String from, String to, int amount) {
    }

    int balance(String name) {
        return accounts.getOrDefault(name, 0);
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
