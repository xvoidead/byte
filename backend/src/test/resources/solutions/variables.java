public class Main {
    public static void main(String[] args) {
        int width = 7;
        int height = 3;
        int area = width * height;
        int perimeter = 2 * (width + height);
        boolean longDiagonal = width * width + height * height > 7 * 7;
        System.out.println("Площадь: " + area);
        System.out.println("Периметр: " + perimeter);
        System.out.println("Диагональ больше 7: " + longDiagonal);
    }
}
