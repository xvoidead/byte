import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        long n = new Scanner(System.in).nextLong();
        long rest = Math.abs(n);
        int count = 0;
        long sum = 0;
        long reversed = 0;
        do {
            long digit = rest % 10;
            count++;
            sum += digit;
            reversed = reversed * 10 + digit;
            rest /= 10;
        } while (rest > 0);
        System.out.println("Цифр: " + count);
        System.out.println("Сумма цифр: " + sum);
        System.out.println("Наоборот: " + (n < 0 ? -reversed : reversed));
        System.out.println("Палиндром: " + (reversed == Math.abs(n) ? "да" : "нет"));
    }
}
