import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ClientKeyMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        KeyBinding wave = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.byte.wave", InputUtil.Type.KEYSYM, 71, "category.byte.general"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (wave.wasPressed()) {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("Ты нажал G!"), false);
                }
            }
        });
    }
}
