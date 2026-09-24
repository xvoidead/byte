import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int score = in.nextInt();
        if (score < 0 || score > 100) {
            System.out.println("Ошибка: балл должен быть от 0 до 100");
        } else if (score >= 90) {
            System.out.println("Оценка: 5");
        } else if (score >= 70) {
            System.out.println("Оценка: 4");
        } else if (score >= 50) {
            System.out.println("Оценка: 3");
        } else {
            System.out.println("Оценка: 2");
        }
    }
}
