package dev.byteide.bridge.mongo;

import java.util.concurrent.Executor;

import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;
import com.mongodb.internal.connection.StreamFactory;
import com.mongodb.internal.connection.StreamFactoryFactory;

import de.bwaldvogel.mongo.MongoBackend;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import de.bwaldvogel.mongo.wire.MongoDatabaseHandler;
import de.bwaldvogel.mongo.wire.MongoExceptionHandler;
import de.bwaldvogel.mongo.wire.MongoWireMessageEncoder;
import de.bwaldvogel.mongo.wire.MongoWireProtocolHandler;
import de.bwaldvogel.mongo.wire.MongoWireReplyEncoder;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

/**
 * MongoDB внутри программы ученика: сервер mongo-java-server с хранилищем в памяти, к которому драйвер
 * подключается не по сети, а через {@link InMemoryStream}. Каждое соединение драйвера — отдельный
 * {@link EmbeddedChannel} с тем же конвейером обработчиков, что и у настоящего сервера mongo-java-server.
 * Данные живут, пока работает программа.
 */
public final class InMemoryMongo {

    static {
        // Netty сам выбирает журнал и, увидев заглушку slf4j-nop, пишет в java.util.logging — прямо в вывод
        // программы ученика. Направляем его в slf4j, как и остальные библиотеки.
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
    }

    private InMemoryMongo() {
    }

    /** Фабрика соединений для драйвера — подставляется в {@code MongoClients}. */
    public static StreamFactoryFactory streamFactoryFactory() {
        return new StreamFactoryFactory() {
            @Override
            public StreamFactory create(SocketSettings socketSettings, SslSettings sslSettings) {
                return InMemoryStream::new;
            }

            @Override
            public Executor getExecutor() {
                throw new UnsupportedOperationException("Асинхронный драйвер в песочнице не поддерживается");
            }

            @Override
            public void close() {
                // соединения закрывает драйвер
            }
        };
    }

    static EmbeddedChannel openChannel() {
        EmbeddedChannel channel = new EmbeddedChannel();
        // Пул буферов Netty регистрирует события JFR, а это песочница запрещает. Без пула проще и хватает.
        channel.config().setAllocator(UnpooledByteBufAllocator.DEFAULT);
        channel.pipeline().addLast(
                new MongoWireReplyEncoder(),
                new MongoWireMessageEncoder(),
                new MongoWireProtocolHandler(),
                new MongoDatabaseHandler(Server.BACKEND, Server.CHANNELS),
                new MongoExceptionHandler());
        return channel;
    }

    /** Сервер создаётся при первом соединении. */
    private static final class Server {
        static final MongoBackend BACKEND = new MemoryBackend();
        static final ChannelGroup CHANNELS = new DefaultChannelGroup(ImmediateEventExecutor.INSTANCE);
    }
}
