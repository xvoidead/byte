package dev.byteide.lessons;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SolutionCheckerTest {

    @Test
    void normalizesLineEndingsAndTrailingWhitespace() {
        assertThat(SolutionChecker.normalize("a  \r\nb\r\n\n\n")).isEqualTo(SolutionChecker.normalize("a\nb"));
        assertThat(SolutionChecker.normalize("a\n b")).isNotEqualTo(SolutionChecker.normalize("a\nb"));
    }
}
