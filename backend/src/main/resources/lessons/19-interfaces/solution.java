import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

interface Discount {
    int apply(int price);

    String describe();
}

class PercentDiscount implements Discount {
    private final int percent;

    PercentDiscount(int percent) {
        this.percent = percent;
    }

    @Override
    public int apply(int price) {
        return price - price * percent / 100;
    }

    @Override
    public String describe() {
        return "Скидка " + percent + "%";
    }
}

class FixedDiscount implements Discount {
    private final int amount;

    FixedDiscount(int amount) {
        this.amount = amount;
    }

    @Override
    public int apply(int price) {
        return Math.max(0, price - amount);
    }

    @Override
    public String describe() {
        return "Скидка " + amount + " ₽";
    }
}

class NoDiscount implements Discount {
    @Override
    public int apply(int price) {
        return price;
    }

    @Override
    public String describe() {
        return "Без скидки";
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int price = in.nextInt();
        List<Discount> discounts = new ArrayList<>();
        while (in.hasNext()) {
            String type = in.next();
            discounts.add(switch (type) {
                case "percent" -> new PercentDiscount(in.nextInt());
                case "fixed" -> new FixedDiscount(in.nextInt());
                default -> new NoDiscount();
            });
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
