package dev.byteide.bridge.mongo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.bson.ByteBuf;
import org.bson.ByteBufNIO;

import com.mongodb.ServerAddress;
import com.mongodb.connection.AsyncCompletionHandler;
import com.mongodb.internal.connection.OperationContext;
import com.mongodb.internal.connection.Stream;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

/**
 * Соединение драйвера MongoDB с сервером в памяти. Запрос целиком передаётся в канал сервера,
 * ответы сервера копятся в буфере, откуда драйвер читает их нужными ему порциями.
 */
final class InMemoryStream implements Stream {

    private final ServerAddress address;
    private EmbeddedChannel channel;
    private byte[] pending = new byte[0];
    private int pendingStart;
    private boolean closed;

    InMemoryStream(ServerAddress address) {
        this.address = address;
    }

    @Override
    public ByteBuf getBuffer(int size) {
        return new ByteBufNIO(ByteBuffer.allocate(size));
    }

    @Override
    public synchronized void open(OperationContext operationContext) {
        channel = InMemoryMongo.openChannel();
    }

    @Override
    public void openAsync(OperationContext operationContext, AsyncCompletionHandler<Void> handler) {
        open(operationContext);
        handler.completed(null);
    }

    @Override
    public synchronized void write(List<ByteBuf> buffers, OperationContext operationContext) throws IOException {
        ensureOpen();
        io.netty.buffer.ByteBuf request = Unpooled.buffer();
        for (ByteBuf buffer : buffers) {
            request.writeBytes(buffer.array(), 0, buffer.limit());
        }
        channel.writeInbound(request);
        ByteArrayOutputStream replies = new ByteArrayOutputStream();
        replies.write(pending, pendingStart, pending.length - pendingStart);
        Object reply;
        while ((reply = channel.readOutbound()) != null) {
            io.netty.buffer.ByteBuf buf = (io.netty.buffer.ByteBuf) reply;
            try {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                replies.write(bytes);
            } finally {
                buf.release();
            }
        }
        pending = replies.toByteArray();
        pendingStart = 0;
        notifyAll();
    }

    @Override
    public synchronized ByteBuf read(int numBytes, OperationContext operationContext) throws IOException {
        while (!closed && pending.length - pendingStart < numBytes) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Чтение прервано", e);
            }
        }
        ensureOpen();
        ByteBuffer result = ByteBuffer.allocate(numBytes);
        result.put(pending, pendingStart, numBytes).flip();
        pendingStart += numBytes;
        return new ByteBufNIO(result);
    }

    @Override
    public void writeAsync(List<ByteBuf> buffers, OperationContext operationContext,
                           AsyncCompletionHandler<Void> handler) {
        try {
            write(buffers, operationContext);
            handler.completed(null);
        } catch (IOException e) {
            handler.failed(e);
        }
    }

    @Override
    public void readAsync(int numBytes, OperationContext operationContext, AsyncCompletionHandler<ByteBuf> handler) {
        try {
            handler.completed(read(numBytes, operationContext));
        } catch (IOException e) {
            handler.failed(e);
        }
    }

    @Override
    public ServerAddress getAddress() {
        return address;
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            if (channel != null) {
                channel.close();
            }
            notifyAll();
        }
    }

    @Override
    public synchronized boolean isClosed() {
        return closed;
    }

    private void ensureOpen() throws IOException {
        if (closed || channel == null) {
            throw new IOException("Соединение с MongoDB закрыто");
        }
    }
}
