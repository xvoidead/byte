package dev.byteide.sandbox;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Точка входа дочерней JVM. Загружает классы ученика отдельным загрузчиком, включает SecurityManager
 * и только потом вызывает {@code main}. Политика безопасности (файл передаётся сервером) даёт полные права
 * лишь этому лаунчеру, а коду ученика — ничего: ни файлов, ни сети, ни процессов, ни переменных окружения.
 *
 * <p>Класс копируется сервером в отдельный каталог и выполняется в чужой JVM, поэтому зависит только от JDK
 * и не использует анонимные классы (их файлы пришлось бы копировать отдельно).
 *
 * <p>Аргументы: имя главного класса, каталог с классами ученика, максимальное число потоков.
 */
public final class SandboxLauncher {

    /** Метка в stderr: всё, что JVM напечатала до неё (предупреждения о SecurityManager), сервер отбрасывает. */
    public static final String READY_MARKER = "\u0001byte-sandbox-ready\u0001";

    /** Код выхода, если песочницу включить не удалось. Сервер никогда не запускает код без неё. */
    public static final int SANDBOX_UNAVAILABLE_EXIT = 97;

    private SandboxLauncher() {
    }

    public static void main(String[] args) throws Exception {
        String mainClassName = args[0];
        File classesDir = new File(args[1]);
        int maxThreads = Integer.parseInt(args[2]);

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toURI().toURL()},
                ClassLoader.getPlatformClassLoader());
        Method main = loader.loadClass(mainClassName).getMethod("main", String[].class);
        // Обычный запуск java умеет вызывать main у непубличного класса — разрешаем и мы, пока защита не включена.
        main.setAccessible(true);

        PrintStream err = System.err;
        try {
            System.setSecurityManager(new Guard(maxThreads));
        } catch (UnsupportedOperationException | SecurityException e) {
            err.println("Песочница недоступна: " + e);
            err.flush();
            System.exit(SANDBOX_UNAVAILABLE_EXIT);
        }
        err.println(READY_MARKER);
        err.flush();

        Thread.currentThread().setContextClassLoader(loader);
        try {
            main.invoke(null, (Object) new String[0]);
        } catch (InvocationTargetException e) {
            printUncaught(e.getCause(), err);
            System.out.flush();
            System.exit(1);
        }
    }

    /** Печатает исключение как стандартный обработчик JVM, но без кадров лаунчера и со свёрткой рекурсии. */
    static void printUncaught(Throwable error, PrintStream out) {
        StringBuilder sb = new StringBuilder("Exception in thread \"main\" ");
        appendThrowable(sb, error, new IdentityHashMap<>());
        out.print(sb);
        out.flush();
    }

    private static void appendThrowable(StringBuilder sb, Throwable error, Map<Throwable, Boolean> seen) {
        seen.put(error, Boolean.TRUE);
        sb.append(error).append('\n');
        appendFrames(sb, userFrames(error.getStackTrace()));
        Throwable cause = error.getCause();
        if (cause != null && !seen.containsKey(cause)) {
            sb.append("Caused by: ");
            appendThrowable(sb, cause, seen);
        }
    }

    /** Убирает нижние кадры рефлексии и самого лаунчера — ученику они ничего не говорят. */
    static StackTraceElement[] userFrames(StackTraceElement[] frames) {
        int end = frames.length;
        while (end > 0 && isInfrastructure(frames[end - 1])) {
            end--;
        }
        StackTraceElement[] result = new StackTraceElement[end];
        System.arraycopy(frames, 0, result, 0, end);
        return result;
    }

    private static boolean isInfrastructure(StackTraceElement frame) {
        String cls = frame.getClassName();
        return cls.startsWith("jdk.internal.reflect.")
                || cls.startsWith("java.lang.reflect.")
                || cls.startsWith("dev.byteide.sandbox.");
    }

    /**
     * Выводит кадры, сворачивая повторяющиеся блоки (типично для StackOverflowError):
     * вместо тысячи одинаковых строк — одна и пометка, сколько раз она повторилась.
     */
    static void appendFrames(StringBuilder sb, StackTraceElement[] frames) {
        int i = 0;
        while (i < frames.length) {
            int bestPeriod = 0;
            int bestRepeats = 0;
            for (int period = 1; period <= 5 && i + period <= frames.length; period++) {
                int repeats = 1;
                while (i + (repeats + 1) * period <= frames.length && sameBlock(frames, i, i + repeats * period, period)) {
                    repeats++;
                }
                if (repeats > bestRepeats && repeats >= 4) {
                    bestRepeats = repeats;
                    bestPeriod = period;
                }
            }
            if (bestPeriod > 0) {
                for (int k = 0; k < bestPeriod; k++) {
                    sb.append("\tat ").append(frames[i + k]).append('\n');
                }
                sb.append("\t... ещё ").append((bestRepeats - 1) * bestPeriod)
                        .append(" таких же вызовов (").append(bestRepeats - 1).append(" повторений)\n");
                i += bestRepeats * bestPeriod;
            } else {
                sb.append("\tat ").append(frames[i]).append('\n');
                i++;
            }
        }
    }

    private static boolean sameBlock(StackTraceElement[] frames, int a, int b, int length) {
        for (int k = 0; k < length; k++) {
            if (!frames[a + k].equals(frames[b + k])) {
                return false;
            }
        }
        return true;
    }

    /** Разрешает только то, что даёт политика, и ограничивает число потоков ученика. */
    @SuppressWarnings("removal")
    static final class Guard extends SecurityManager {
        private final int maxThreads;

        Guard(int maxThreads) {
            this.maxThreads = maxThreads;
        }

        @Override
        public void checkAccess(ThreadGroup group) {
            super.checkAccess(group);
            // Вызывается при создании каждого потока: не даём заполнить память тысячами потоков.
            if (group.activeCount() >= maxThreads) {
                throw new SecurityException("Слишком много потоков: в песочнице можно не больше " + maxThreads);
            }
        }
    }

    /** Список классов лаунчера, которые сервер копирует в каталог песочницы. */
    public static List<Class<?>> classesToCopy() {
        List<Class<?>> classes = new ArrayList<>();
        classes.add(SandboxLauncher.class);
        classes.add(Guard.class);
        return classes;
    }
}
