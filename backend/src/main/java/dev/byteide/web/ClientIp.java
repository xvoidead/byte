package dev.byteide.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Адрес клиента. За обратным прокси нужно включить {@code server.forward-headers-strategy=native},
 * тогда Tomcat подставит адрес из X-Forwarded-For (только от доверенных внутренних прокси).
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /** Адрес без последнего октета — для логов, чтобы не хранить точные IP. */
    public static String masked(String ip) {
        int dot = ip.lastIndexOf('.');
        if (dot > 0) {
            return ip.substring(0, dot) + ".x";
        }
        int colon = ip.lastIndexOf(':');
        return colon > 0 ? ip.substring(0, colon) + ":x" : "x";
    }
}
