package net.fabricmc.fabric.api.client.keybinding.v1;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.option.KeyBinding;

/** Key-binding helper subset used in Byte's Fabric client lessons. */
public final class KeyBindingHelper {

    private static final List<KeyBinding> BINDINGS = new ArrayList<>();

    private KeyBindingHelper() {
    }

    public static KeyBinding registerKeyBinding(KeyBinding binding) {
        BINDINGS.add(binding);
        return binding;
    }

    /** Test-runtime hook; not part of Fabric API. */
    public static void pressKey(int keyCode) {
        BINDINGS.stream().filter(binding -> binding.getKeyCode() == keyCode).forEach(KeyBinding::queuePress);
    }

    /** Test-runtime hook; not part of Fabric API. */
    public static void reset() {
        BINDINGS.clear();
    }
}
