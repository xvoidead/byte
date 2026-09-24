import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    static MongoCollection<Document> players;

    public static void main(String[] args) {
        try (MongoClient client = MongoClients.create("mongodb://localhost:27017")) {
            players = client.getDatabase("game").getCollection("players");
            Scanner in = new Scanner(System.in);
            while (in.hasNextLine()) {
                String[] parts = in.nextLine().strip().split("\\s+", 3);
                switch (parts[0]) {
                    case "join" -> join(parts[1]);
                    case "reward" -> reward(parts[1], Integer.parseInt(parts[2]));
                    case "give" -> give(parts[1], parts[2]);
                    case "levelup" -> levelUp(parts[1]);
                    case "profile" -> profile(parts[1]);
                    case "top" -> top();
                    case "rich" -> rich(Integer.parseInt(parts[1]));
                    default -> { }
                }
            }
        }
    }

    static void join(String name) {
        if (players.find(Filters.eq("name", name)).first() != null) {
            System.out.println("С возвращением, " + name + "!");
            return;
        }
        players.insertOne(new Document("name", name)
                .append("level", 1)
                .append("coins", 0)
                .append("items", new ArrayList<String>()));
        System.out.println("Новый игрок: " + name);
    }

    static void profile(String name) {
        Document player = players.find(Filters.eq("name", name)).first();
        if (player == null) {
            System.out.println("Нет игрока " + name);
            return;
        }
        List<String> items = player.getList("items", String.class);
        System.out.println(name + ": уровень " + player.getInteger("level") + ", монет " + player.getInteger("coins")
                + ", " + (items.isEmpty() ? "предметов нет" : "предметы: " + String.join(", ", items)));
    }

    static void reward(String name, int coins) {
        // TODO: Updates.inc
    }

    static void give(String name, String item) {
        // TODO: Updates.addToSet — и отличите «уже есть» от «нет игрока»
    }

    static void levelUp(String name) {
        // TODO: findOneAndUpdate с ReturnDocument.AFTER
    }

    static void top() {
        // TODO: find().sort(...).limit(3)
    }

    static void rich(int minCoins) {
        // TODO: Filters.gte и сортировка по имени
    }
}
