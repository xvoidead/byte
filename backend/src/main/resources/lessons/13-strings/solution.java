import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        String line = new Scanner(System.in).nextLine();
        String reversed = new StringBuilder(line).reverse().toString();
        String normalized = line.replace(" ", "").toLowerCase();
        boolean palindrome = normalized.equals(new StringBuilder(normalized).reverse().toString());
        System.out.println("Длина: " + line.length());
        System.out.println("Наоборот: " + reversed);
        System.out.println("Палиндром: " + (palindrome ? "да" : "нет"));
    }
}
