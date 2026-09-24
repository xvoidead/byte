import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

record Participant(String name, int score) {
}

public class Main {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        List<Participant> participants = new ArrayList<>();
        while (in.hasNext()) {
            participants.add(new Participant(in.next(), in.nextInt()));
        }

        // Отсортируйте участников

        for (int i = 0; i < participants.size(); i++) {
            Participant p = participants.get(i);
            System.out.println((i + 1) + ". " + p.name() + " — " + p.score());
        }
    }
}
