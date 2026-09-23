package dev.byteide.web;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/** Не даёт прислать гигантское тело запроса: оно отклоняется до того, как будет прочитано в память. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestSizeFilter extends OncePerRequestFilter {

    private final int maxBytes;

    public RequestSizeFilter(LimitsProperties limits) {
        this.maxBytes = limits.maxRequestBytes();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxBytes) {
            response.setStatus(HttpStatus.CONTENT_TOO_LARGE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Слишком большой запрос.\"}");
            return;
        }
        chain.doFilter(new LimitedRequest(request, maxBytes), response);
    }

    /** Для запросов без Content-Length (chunked) обрывает чтение на лимите. */
    private static final class LimitedRequest extends HttpServletRequestWrapper {
        private final int maxBytes;

        LimitedRequest(HttpServletRequest request, int maxBytes) {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            ServletInputStream delegate = super.getInputStream();
            return new ServletInputStream() {
                private long read;

                @Override
                public int read() throws IOException {
                    int b = delegate.read();
                    if (b != -1) {
                        count(1);
                    }
                    return b;
                }

                @Override
                public int read(byte[] buffer, int offset, int length) throws IOException {
                    int n = delegate.read(buffer, offset, length);
                    if (n > 0) {
                        count(n);
                    }
                    return n;
                }

                private void count(int n) throws IOException {
                    read += n;
                    if (read > maxBytes) {
                        throw new IOException("Тело запроса больше " + maxBytes + " байт");
                    }
                }

                @Override
                public boolean isFinished() {
                    return delegate.isFinished();
                }

                @Override
                public boolean isReady() {
                    return delegate.isReady();
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    delegate.setReadListener(listener);
                }
            };
        }
    }
}
