import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.List;

record Sale(String product, int price, int quantity) {
    static Sale parse(String line) {
        String[] parts = line.trim().split("\\s+");
        return new Sale(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
}

public class Main {
    public static void main(String[] args) {
        List<Sale> sales = new BufferedReader(new InputStreamReader(System.in)).lines()
                .filter(line -> !line.isBlank())
                .map(Sale::parse)
                .toList();

        // Посчитайте выручку, самый дорогой товар, число дорогих продаж и список товаров

        System.out.println("Выручка: " + 0);
    }
}
