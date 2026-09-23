import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        long a = in.nextLong();
        long b = in.nextLong();
        System.out.println("Сумма: " + (a + b));
        System.out.println("Произведение: " + a * b);
    }
}
