package dev.byteide.lessons;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбирает теорию урока: каждый заголовок второго уровня ({@code ## }) начинает новый шаг,
 * а блоки {@code ```quiz} превращаются в вопросы.
 *
 * <p>Формат вопроса:
 * <pre>
 * ```quiz
 * ? Что выведет программа?
 *     int a = 7 / 2;
 *     System.out.println(a);
 * - 3.5
 * + 3
 * - Ошибка компиляции
 * > Деление целых чисел отбрасывает дробную часть.
 * ```
 * </pre>
 * Строки с отступом в 4 пробела — код, «+» — правильный вариант, «-» — неправильный,
 * «>» — объяснение. В вариантах можно писать {@code \n} для перевода строки.
 */
public final class LessonMarkdown {

    static final String INTRO_TITLE = "Введение";

    private LessonMarkdown() {
    }

    public static List<Step> parseSteps(String markdown) {
        List<Step> steps = new ArrayList<>();
        String title = INTRO_TITLE;
        List<Step.Block> blocks = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        boolean inFence = false;
        List<String> quizLines = null;

        for (String line : markdown.replace("\r\n", "\n").split("\n", -1)) {
            if (quizLines != null) {
                if (line.strip().equals("```")) {
                    blocks.add(new Step.Question(parseQuiz(quizLines)));
                    quizLines = null;
                } else {
                    quizLines.add(line);
                }
                continue;
            }
            if (!inFence && line.strip().equals("```quiz")) {
                flushText(text, blocks);
                quizLines = new ArrayList<>();
                continue;
            }
            if (line.startsWith("```")) {
                inFence = !inFence;
            }
            if (!inFence && line.startsWith("## ")) {
                flushText(text, blocks);
                if (!blocks.isEmpty()) {
                    steps.add(new Step(title, List.copyOf(blocks)));
                }
                blocks.clear();
                title = line.substring(3).strip();
                continue;
            }
            text.append(line).append('\n');
        }
        if (quizLines != null) {
            throw new IllegalArgumentException("Не закрыт блок ```quiz");
        }
        flushText(text, blocks);
        if (!blocks.isEmpty()) {
            steps.add(new Step(title, List.copyOf(blocks)));
        }
        return List.copyOf(steps);
    }

    private static void flushText(StringBuilder text, List<Step.Block> blocks) {
        String markdown = text.toString().strip();
        if (!markdown.isEmpty()) {
            blocks.add(new Step.Text(markdown));
        }
        text.setLength(0);
    }

    static Quiz parseQuiz(List<String> lines) {
        StringBuilder question = new StringBuilder();
        StringBuilder code = new StringBuilder();
        StringBuilder explanation = new StringBuilder();
        List<String> options = new ArrayList<>();
        int answer = -1;
        for (String line : lines) {
            if (line.startsWith("    ")) {
                code.append(line.substring(4)).append('\n');
            } else if (line.startsWith("? ")) {
                append(question, line.substring(2).strip());
            } else if (line.startsWith("+ ") || line.startsWith("- ")) {
                if (line.startsWith("+ ")) {
                    if (answer >= 0) {
                        throw new IllegalArgumentException("В вопросе больше одного правильного ответа: " + question);
                    }
                    answer = options.size();
                }
                options.add(line.substring(2).strip().replace("\\n", "\n"));
            } else if (line.startsWith("> ")) {
                append(explanation, line.substring(2).strip());
            } else if (!line.isBlank()) {
                throw new IllegalArgumentException("Непонятная строка в вопросе «" + question + "»: " + line);
            }
        }
        if (question.isEmpty() || options.size() < 2 || answer < 0) {
            throw new IllegalArgumentException("В вопросе нужны текст, хотя бы два варианта и один правильный: " + question);
        }
        String codeText = code.toString().stripTrailing();
        return new Quiz(question.toString(), codeText.isEmpty() ? null : codeText, List.copyOf(options), answer,
                explanation.toString());
    }

    private static void append(StringBuilder sb, String text) {
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(text);
    }
}
