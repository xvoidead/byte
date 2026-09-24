package dev.byteide.lessons;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class RandomInputTest {

    private final RandomGenerator random = RandomGenerator.of("L64X128MixRandom");

    @RepeatedTest(20)
    void generatesArrayWithMatchingLength() {
        String input = new RandomInput("{n=int:1..10}\n{ints:n:-5..5}\n").render(random);
        String[] lines = input.split("\n");
        int n = Integer.parseInt(lines[0]);
        int[] values = Arrays.stream(lines[1].split(" ")).mapToInt(Integer::parseInt).toArray();

        assertThat(values).hasSize(n);
        assertThat(Arrays.stream(values).boxed().toList()).allMatch(v -> v >= -5 && v <= 5);
    }

    @RepeatedTest(20)
    void supportsNestedChoicesRepeatAndMixcase() {
        String input = new RandomInput("{repeat:2..3:{choice:a|{word:2..2}} }{mixcase:{words:3..3:xy|zz}}").render(random);

        assertThat(input).matches("([a-z]{1,2} ){2,3}[xyzXYZ]{2}( [xyzXYZ]{2}){2}");
    }

    @Test
    void rejectsBrokenTemplates() {
        assertThatThrownBy(() -> new RandomInput("{int:5..1}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RandomInput("{nope:1}")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RandomInput("{int:1..2")).isInstanceOf(IllegalArgumentException.class);
    }
}
