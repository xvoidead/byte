package dev.byteide.bridge.postgres;

import io.roastedroot.pglite4j.core.PGLite;

/**
 * Настоящий PostgreSQL, скомпилированный в WebAssembly (PGlite), внутри программы ученика.
 * Запускается при первом подключении; база в памяти и пропадает, когда программа завершится.
 *
 * <p>PGlite — однопользовательский сервер с одной сессией. Все соединения программы работают в ней по очереди,
 * а приветствие сервера после первого подключения запоминается и повторяется для следующих.
 */
final class PgliteEngine {

    private static final Object LOCK = new Object();
    private static PGLite pg;
    private static byte[] startupReply;

    private PgliteEngine() {
    }

    /**
     * Приветствие сервера. PGlite просит пароль, но принимает любой, поэтому мост сам отвечает ему и отдаёт
     * драйверу уже готовый ответ: AuthenticationOk, параметры сервера и ReadyForQuery — как сервер с входом
     * без пароля (trust). Имя пользователя, пароль и база в адресе подключения ни на что не влияют.
     */
    static byte[] startup(byte[] startupMessage) {
        synchronized (LOCK) {
            if (startupReply == null) {
                byte[] reply = engine().execProtocolRaw(startupMessage);
                if (isPasswordRequest(reply)) {
                    reply = engine().execProtocolRaw(PASSWORD_MESSAGE);
                }
                startupReply = reply;
            }
            return startupReply;
        }
    }

    /** Сообщение PasswordMessage с произвольным паролем. */
    private static final byte[] PASSWORD_MESSAGE = {'p', 0, 0, 0, 9, 'b', 'y', 't', 'e', 0};

    /** Ответ — запрос аутентификации ('R' с ненулевым кодом). */
    private static boolean isPasswordRequest(byte[] reply) {
        return reply.length >= 9 && reply[0] == 'R'
                && (reply[5] | reply[6] | reply[7] | reply[8]) != 0;
    }

    static byte[] exec(byte[] messages) {
        synchronized (LOCK) {
            return engine().execProtocolRaw(messages);
        }
    }

    private static PGLite engine() {
        if (pg == null) {
            pg = PGLite.builder().build();
        }
        return pg;
    }
}
