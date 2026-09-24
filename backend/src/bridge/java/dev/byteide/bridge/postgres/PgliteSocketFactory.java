package dev.byteide.bridge.postgres;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.SocketFactory;

/**
 * Фабрика сокетов для драйвера PostgreSQL (pgjdbc). Подключается через {@code org/postgresql/driverconfig.properties}
 * в песочнице, поэтому ученик пишет обычный {@code DriverManager.getConnection("jdbc:postgresql://localhost/shop")},
 * а соединение приходит в PostgreSQL, работающий внутри программы (PGlite).
 */
public final class PgliteSocketFactory extends SocketFactory {

    @Override
    public Socket createSocket() throws IOException {
        return new PgliteSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = createSocket();
        socket.connect(null);
        return socket;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket(host.getHostName(), port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
            throws IOException {
        return createSocket(address.getHostName(), port);
    }
}
