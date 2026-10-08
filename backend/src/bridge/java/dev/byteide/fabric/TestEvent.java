package dev.byteide.fabric;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Small deterministic event bus used by Byte's Fabric lesson simulator. */
public final class TestEvent<T> {

    private final List<T> listeners = new ArrayList<>();

    public void register(T listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void invoke(Consumer<? super T> action) {
        for (T listener : List.copyOf(listeners)) {
            action.accept(listener);
        }
    }

    public void clear() {
        listeners.clear();
    }
}
