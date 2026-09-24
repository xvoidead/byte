package dev.byteide.bridge.postgres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;

/**
 * «Сокет» без сети: всё, что драйвер пишет, при {@code flush()} уходит в {@link PgliteEngine},
 * а ответ сервера становится доступен для чтения. Драйвер всегда сбрасывает буфер на границе сообщений
 * протокола, поэтому движок получает только целые сообщения.
 */
final class PgliteSocket extends Socket {

    private static final int SSL_REQUEST = 80877103;
    private static final int GSS_REQUEST = 80877104;
    private static final int CANCEL_REQUEST = 80877102;

    private final Input input = new Input();
    private final Output output = new Output();
    private boolean connected;
    private boolean closed;
    private boolean started;
    private int soTimeout;

    PgliteSocket() throws SocketException {
        super((SocketImpl) null);
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        connect(endpoint, 0);
    }

    @Override
    public synchronized void connect(SocketAddress endpoint, int timeout) throws IOException {
        if (endpoint instanceof InetSocketAddress address && !isLocal(address)) {
            throw new SocketException("В песочнице нет сети: PostgreSQL доступен только на localhost, а не на "
                    + address.getHostString());
        }
        connected = true;
    }

    private static boolean isLocal(InetSocketAddress address) {
        String host = address.getHostString();
        return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")
                || (address.getAddress() != null && address.getAddress().isLoopbackAddress());
    }

    /** Обрабатывает накопленные сообщения клиента и кладёт ответ в буфер чтения. */
    private synchronized void process(byte[] data) throws IOException {
        int pos = 0;
        ByteArrayOutputStream forward = new ByteArrayOutputStream();
        while (pos < data.length) {
            if (!started) {
                // Сообщения до начала сессии идут без байта типа: длина и код.
                int length = readInt(data, pos);
                int code = readInt(data, pos + 4);
                if (code == SSL_REQUEST || code == GSS_REQUEST) {
                    input.append(new byte[] {'N'});
                } else if (code == CANCEL_REQUEST) {
                    close();
                    return;
                } else {
                    byte[] startup = new byte[length];
                    System.arraycopy(data, pos, startup, 0, length);
                    input.append(PgliteEngine.startup(startup));
                    started = true;
                }
                pos += length;
                continue;
            }
            byte type = data[pos];
            int length = readInt(data, pos + 1) + 1;
            if (type == 'X') {
                // Terminate закрыл бы единственную сессию PostgreSQL, общую для всех соединений программы.
                flushTo(forward);
                closed = true;
                return;
            }
            forward.write(data, pos, length);
            pos += length;
        }
        flushTo(forward);
    }

    private void flushTo(ByteArrayOutputStream forward) throws IOException {
        if (forward.size() > 0) {
            input.append(PgliteEngine.exec(forward.toByteArray()));
            forward.reset();
        }
    }

    private static int readInt(byte[] data, int pos) throws IOException {
        if (pos + 4 > data.length) {
            throw new IOException("Неполное сообщение протокола PostgreSQL");
        }
        return ((data[pos] & 0xff) << 24) | ((data[pos + 1] & 0xff) << 16) | ((data[pos + 2] & 0xff) << 8)
                | (data[pos + 3] & 0xff);
    }

    private final class Input extends InputStream {
        private byte[] buffer = new byte[0];
        private int start;

        void append(byte[] bytes) {
            if (bytes.length == 0) {
                return;
            }
            byte[] next = new byte[buffer.length - start + bytes.length];
            System.arraycopy(buffer, start, next, 0, buffer.length - start);
            System.arraycopy(bytes, 0, next, buffer.length - start, bytes.length);
            buffer = next;
            start = 0;
        }

        @Override
        public int read() {
            synchronized (PgliteSocket.this) {
                return start < buffer.length ? buffer[start++] & 0xff : -1;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) {
            synchronized (PgliteSocket.this) {
                if (len == 0) {
                    return 0;
                }
                int available = buffer.length - start;
                if (available == 0) {
                    // Ответ на каждый запрос готов сразу после flush: если данных нет, их уже не будет.
                    return -1;
                }
                int n = Math.min(len, available);
                System.arraycopy(buffer, start, b, off, n);
                start += n;
                return n;
            }
        }

        @Override
        public int available() {
            synchronized (PgliteSocket.this) {
                return buffer.length - start;
            }
        }
    }

    private final class Output extends OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public void write(int b) {
            buffer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            buffer.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            byte[] data = buffer.toByteArray();
            buffer.reset();
            if (data.length == 0 || isClosed()) {
                // После Terminate драйвер ещё раз сбрасывает буфер при закрытии — как и сокет, молча отбрасываем.
                return;
            }
            process(data);
        }
    }

    @Override
    public InputStream getInputStream() {
        return input;
    }

    @Override
    public OutputStream getOutputStream() {
        return output;
    }

    @Override
    public synchronized boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isBound() {
        return connected;
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        closed = true;
    }

    @Override
    public void shutdownInput() {
    }

    @Override
    public void shutdownOutput() {
    }

    @Override
    public boolean isInputShutdown() {
        return closed;
    }

    @Override
    public boolean isOutputShutdown() {
        return closed;
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        throw new SocketException("В песочнице нет сети");
    }

    @Override
    public void setTcpNoDelay(boolean on) {
    }

    @Override
    public boolean getTcpNoDelay() {
        return true;
    }

    @Override
    public void setKeepAlive(boolean on) {
    }

    @Override
    public boolean getKeepAlive() {
        return false;
    }

    @Override
    public void setSoLinger(boolean on, int linger) {
    }

    @Override
    public int getSoLinger() {
        return -1;
    }

    @Override
    public synchronized void setSoTimeout(int timeout) {
        soTimeout = timeout;
    }

    @Override
    public synchronized int getSoTimeout() {
        return soTimeout;
    }

    @Override
    public void setSendBufferSize(int size) {
    }

    @Override
    public int getSendBufferSize() {
        return 65536;
    }

    @Override
    public void setReceiveBufferSize(int size) {
    }

    @Override
    public int getReceiveBufferSize() {
        return 65536;
    }

    @Override
    public InetAddress getInetAddress() {
        return InetAddress.getLoopbackAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return InetAddress.getLoopbackAddress();
    }

    @Override
    public int getPort() {
        return 5432;
    }

    @Override
    public int getLocalPort() {
        return -1;
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return new InetSocketAddress(InetAddress.getLoopbackAddress(), 5432);
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return null;
    }

    @Override
    public String toString() {
        return "PgliteSocket[localhost:5432]";
    }
}
