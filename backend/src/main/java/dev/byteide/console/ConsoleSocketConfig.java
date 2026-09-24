package dev.byteide.console;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import dev.byteide.web.ClientIp;

/**
 * WebSocket консоли. Подключаться можно только со своего сайта (проверка Origin);
 * для разработки через Vite дополнительно разрешены адреса из {@code byte.console.dev-origins}.
 */
@Configuration
@EnableWebSocket
public class ConsoleSocketConfig implements WebSocketConfigurer {

    private final ConsoleSocketHandler handler;
    private final ConsoleProperties properties;

    public ConsoleSocketConfig(ConsoleSocketHandler handler, ConsoleProperties properties) {
        this.handler = handler;
        this.properties = properties;
    }

    @ConfigurationProperties("byte.console")
    public record ConsoleProperties(@DefaultValue("http://localhost:5173") List<String> devOrigins) {
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/console")
                .addInterceptors(new ClientIpInterceptor())
                .setAllowedOrigins(properties.devOrigins().toArray(String[]::new));
    }

    /** Запоминает IP клиента из HTTP-запроса установки соединения — для лимитов. */
    static final class ClientIpInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) {
            if (request instanceof ServletServerHttpRequest servlet) {
                attributes.put(ConsoleSocketHandler.IP_ATTRIBUTE, ClientIp.of(servlet.getServletRequest()));
            }
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}
