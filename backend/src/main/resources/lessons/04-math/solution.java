import java.util.Locale;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int a = in.nextInt();
        int b = in.nextInt();
        double hypotenuse = Math.sqrt((double) a * a + (double) b * b);
        double area = a * b / 2.0;
        long perimeter = Math.round(a + b + hypotenuse);
        System.out.printf(Locale.US, "Гипотенуза: %.2f%n", hypotenuse);
        System.out.printf(Locale.US, "Площадь: %.2f%n", area);
        System.out.println("Периметр: " + perimeter);
    }
}
