import java.util.Random;
import java.util.Scanner;

public class Main {
    static final int MAX_ATTEMPTS = 7;

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        long seed = Long.parseLong(in.nextLine().trim());
        int secret = new Random(seed).nextInt(100) + 1;
        int attempts = 0;
        while (in.hasNextLine()) {
            String line = in.nextLine().trim();
            int guess;
            try {
                guess = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Введите число от 1 до 100");
                continue;
            }
            if (guess < 1 || guess > 100) {
                System.out.println("Введите число от 1 до 100");
                continue;
            }
            attempts++;
            if (guess == secret) {
                System.out.println("Угадал! Попыток: " + attempts);
                return;
            }
            System.out.println(guess < secret ? "Больше" : "Меньше");
            if (attempts == MAX_ATTEMPTS) {
                System.out.println("Попытки закончились. Было загадано " + secret);
                return;
            }
        }
        System.out.println("Игра прервана");
    }
}
