import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class Main {
    static final Path FILE = Path.of("players.json");

    public static void main(String[] args) throws Exception {
        // TODO: если players.json повреждён — выведите «Файл players.json повреждён»
        //       и завершите программу, ничего не сохраняя.
        List<Player> players = load();

        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String[] parts = in.nextLine().strip().split(" +");
            switch (parts[0]) {
                case "add" -> add(players, parts[1]);
                case "score" -> score(players, parts[1], Integer.parseInt(parts[2]));
                case "top" -> top(players);
                default -> {
                }
            }
        }
        save(players);
        System.out.println("Сохранено игроков: " + players.size());
    }

    /** Игроки из players.json; если файла нет — пустой список. */
    static List<Player> load() throws Exception {
        // TODO: прочитайте файл через Gson — понадобится TypeToken для List<Player>.
        return new ArrayList<>();
    }

    /** Сохраняет игроков в players.json. */
    static void save(List<Player> players) throws Exception {
        // TODO: сохраните красиво, с отступами.
        Files.writeString(FILE, new Gson().toJson(players));
    }

    static void add(List<Player> players, String name) {
        if (find(players, name) >= 0) {
            System.out.println(name + " уже есть");
            return;
        }
        players.add(new Player(name, 0));
        System.out.println("Добавлен " + name);
    }

    static void score(List<Player> players, String name, int points) {
        int index = find(players, name);
        if (index < 0) {
            System.out.println("Нет игрока " + name);
            return;
        }
        Player updated = players.get(index).addScore(points);
        players.set(index, updated);
        System.out.println(name + ": " + updated.score());
    }

    static void top(List<Player> players) {
        if (players.isEmpty()) {
            System.out.println("Игроков нет");
            return;
        }
        List<Player> sorted = new ArrayList<>(players);
        sorted.sort(Comparator.comparingInt(Player::score).reversed());
        for (int i = 0; i < sorted.size(); i++) {
            System.out.println((i + 1) + ". " + sorted.get(i).name() + " — " + sorted.get(i).score());
        }
    }

    static int find(List<Player> players, String name) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }
}
