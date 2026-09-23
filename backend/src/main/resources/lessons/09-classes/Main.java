import java.util.Scanner;

class BankAccount {
    private int balance;

    void deposit(int amount) {
        // Ваш код
    }

    void withdraw(int amount) {
        // Ваш код
    }

    int getBalance() {
        return balance;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        BankAccount account = new BankAccount();

        while (in.hasNext()) {
            String command = in.next();
            switch (command) {
                case "deposit" -> account.deposit(in.nextInt());
                case "withdraw" -> account.withdraw(in.nextInt());
                case "balance" -> System.out.println("Баланс: " + account.getBalance());
                default -> System.out.println("Неизвестная команда: " + command);
            }
        }
    }
}
