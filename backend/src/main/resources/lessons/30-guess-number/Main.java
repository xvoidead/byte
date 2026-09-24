import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        long seed = Long.parseLong(in.nextLine().trim());
        int secret = new Random(seed).nextInt(100) + 1;

        // Напишите игровой цикл

        System.out.println("Игра прервана");
    }
}
