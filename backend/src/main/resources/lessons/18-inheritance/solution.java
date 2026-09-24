import java.util.Locale;
import java.util.Scanner;

abstract class Shape {
    abstract double area();

    abstract String name();
}

class Circle extends Shape {
    private final int radius;

    Circle(int radius) {
        this.radius = radius;
    }

    @Override
    double area() {
        return Math.PI * radius * radius;
    }

    @Override
    String name() {
        return "Круг";
    }
}

class Rectangle extends Shape {
    private final int width;
    private final int height;

    Rectangle(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    double area() {
        return (double) width * height;
    }

    @Override
    String name() {
        return "Прямоугольник";
    }
}

class Square extends Rectangle {
    Square(int side) {
        super(side, side);
    }

    @Override
    String name() {
        return "Квадрат";
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
