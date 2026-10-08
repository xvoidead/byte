import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.text.Text;

public class FirstJoinMod implements ModInitializer {
    private final Set<String> seen = new HashSet<>();

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            String name = handler.player.getName().getString();
            String key = name.toLowerCase(Locale.ROOT);
            String message = seen.add(key)
                    ? "Добро пожаловать впервые, " + name + "!"
                    : "С возвращением, " + name + "!";
            handler.player.sendMessage(Text.literal(message), false);
        });
    }
}
