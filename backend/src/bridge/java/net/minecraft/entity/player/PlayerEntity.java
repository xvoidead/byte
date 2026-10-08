package net.minecraft.entity.player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import net.minecraft.text.Text;

/** Deterministic player stand-in used only while running Fabric lesson checks. */
public class PlayerEntity {

    private final Text name;
    private final Deque<String> messages = new ArrayDeque<>();

    public PlayerEntity(String name) {
        this.name = Text.literal(Objects.requireNonNull(name));
    }

    public Text getName() {
        return name;
    }

    public void sendMessage(Text message, boolean overlay) {
        messages.addLast(message.getString());
    }

    public String nextMessage() {
        return messages.pollFirst();
    }

    public List<String> drainMessages() {
        List<String> result = new ArrayList<>(messages);
        messages.clear();
        return result;
    }
}
