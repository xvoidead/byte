import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        int n = new Scanner(System.in).nextInt();

        for (int row = 1; row <= n; row++) {
            // Напечатайте пробелы и звёздочки для строки row
            System.out.println("*");
        }
    }
}
