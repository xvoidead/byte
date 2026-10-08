import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;

public class WelcomeConfigMod implements ModInitializer {
    @Override
    public void onInitialize() {
        Path configDirectory = FabricLoader.getInstance().getConfigDir();
        Path configFile = configDirectory.resolve("byte.json");
        Gson gson = new Gson();
        try {
            Files.createDirectories(configDirectory);
            if (!Files.exists(configFile)) {
                Files.writeString(configFile, gson.toJson(new ModConfig()));
            }
            ModConfig config = gson.fromJson(Files.readString(configFile), ModConfig.class);
            if (config == null) {
                config = new ModConfig();
            }
            ModConfig settings = config;
            ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
                if (!settings.enabled) {
                    return;
                }
                String name = handler.player.getName().getString();
                String message = settings.welcome.replace("{player}", name);
                handler.player.sendMessage(Text.literal(message), false);
            });
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось прочитать config/byte.json", e);
        }
    }
}
