import java.util.Locale;
import java.util.Scanner;

abstract class Shape {
    abstract double area();

    abstract String name();
}

// Допишите классы Circle, Rectangle и Square

class Circle extends Shape {
    Circle(int radius) {
    }

    double area() {
        return 0;
    }

    String name() {
        return "?";
    }
}

class Rectangle extends Shape {
    Rectangle(int width, int height) {
    }

    double area() {
        return 0;
    }

    String name() {
        return "?";
    }
}

class Square extends Shape {
    Square(int side) {
    }

    double area() {
        return 0;
    }

    String name() {
        return "?";
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        double total = 0;
        while (in.hasNext()) {
            String type = in.next();
            Shape shape = switch (type) {
                case "circle" -> new Circle(in.nextInt());
                case "rectangle" -> new Rectangle(in.nextInt(), in.nextInt());
                case "square" -> new Square(in.nextInt());
                default -> throw new IllegalArgumentException("Неизвестная фигура: " + type);
            };
            System.out.printf(Locale.US, "%s: %.2f%n", shape.name(), shape.area());
            total += shape.area();
        }
        System.out.printf(Locale.US, "Общая площадь: %.2f%n", total);
    }
}
