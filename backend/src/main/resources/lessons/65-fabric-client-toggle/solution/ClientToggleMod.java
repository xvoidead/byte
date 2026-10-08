import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

public class ClientToggleMod implements ClientModInitializer {
    private boolean hintsEnabled = true;

    @Override
    public void onInitializeClient() {
        KeyBinding toggle = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.byte.toggle_hints", InputUtil.Type.KEYSYM, 71, "category.byte.general"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggle.wasPressed()) {
                hintsEnabled = !hintsEnabled;
                if (client.player != null) {
                    String message = hintsEnabled ? "Подсказки включены." : "Подсказки выключены.";
                    client.player.sendMessage(Text.literal(message), false);
                }
            }
        });
    }
}
