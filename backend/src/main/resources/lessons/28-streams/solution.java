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

        long revenue = sales.stream().mapToLong(s -> (long) s.price() * s.quantity()).sum();
        String mostExpensive = sales.stream()
                .max(Comparator.comparingInt(Sale::price))
                .map(Sale::product)
                .orElse("—");
        long expensive = sales.stream().filter(s -> s.price() > 1000).count();
        List<String> products = sales.stream().map(Sale::product).distinct().sorted().toList();

        System.out.println("Выручка: " + revenue);
        System.out.println("Самый дорогой товар: " + mostExpensive);
        System.out.println("Продаж дороже 1000: " + expensive);
        System.out.println("Товары: " + String.join(", ", products));
    }
}
