package dev.byteide.lessons;

/**
 * Тест задания: программа получает {@code stdin} и должна напечатать {@code expectedOutput}.
 * Скрытые тесты не показываются ученику до проверки, а их данные не раскрываются и после.
 */
public record LessonTest(String name, String stdin, String expectedOutput, boolean hidden) {

    public LessonTest {
        stdin = stdin == null ? "" : stdin;
        expectedOutput = expectedOutput == null ? "" : expectedOutput;
    }
}
