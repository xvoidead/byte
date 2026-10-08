package net.minecraft.text;

import java.util.Objects;

/** Minimal immutable text value for Fabric lessons. */
public final class Text {

    private final String value;

    private Text(String value) {
        this.value = Objects.requireNonNull(value);
    }

    public static Text literal(String value) {
        return new Text(value);
    }

    public String getString() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
