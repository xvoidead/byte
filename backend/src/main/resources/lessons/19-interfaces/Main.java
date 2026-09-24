import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

interface Discount {
    int apply(int price);

    String describe();
}

// Реализуйте PercentDiscount, FixedDiscount и NoDiscount

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int price = in.nextInt();
        List<Discount> discounts = new ArrayList<>();
        while (in.hasNext()) {
            String type = in.next();
            // Создайте нужную скидку и добавьте её в discounts
        }
        int best = price;
        for (Discount discount : discounts) {
            int result = discount.apply(price);
            System.out.println(discount.describe() + ": " + result);
            best = Math.min(best, result);
        }
        System.out.println("Лучшая цена: " + best);
    }
}
