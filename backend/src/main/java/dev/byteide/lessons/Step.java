package dev.byteide.lessons;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Шаг теории: заголовок и блоки — текст и вопросы в том порядке, в каком они написаны. */
public record Step(String title, List<Block> blocks) {

    public sealed interface Block permits Text, Question {
    }

    public record Text(String markdown) implements Block {
        @JsonProperty("type")
        public String type() {
            return "text";
        }
    }

    public record Question(Quiz quiz) implements Block {
        @JsonProperty("type")
        public String type() {
            return "quiz";
        }
    }
}
