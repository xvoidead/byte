package net.minecraft.client.option;

import java.util.Objects;

import net.minecraft.client.util.InputUtil;

/** Minimal key binding matching the Fabric/Yarn constructor shape used in lessons. */
public final class KeyBinding {

    private final String translationKey;
    private final InputUtil.Type type;
    private final int keyCode;
    private final String category;
    private int presses;

    public KeyBinding(String translationKey, InputUtil.Type type, int keyCode, String category) {
        this.translationKey = Objects.requireNonNull(translationKey);
        this.type = Objects.requireNonNull(type);
        this.keyCode = keyCode;
        this.category = Objects.requireNonNull(category);
    }

    public boolean wasPressed() {
        if (presses == 0) {
            return false;
        }
        presses--;
        return true;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public InputUtil.Type getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public void queuePress() {
        presses++;
    }
}
