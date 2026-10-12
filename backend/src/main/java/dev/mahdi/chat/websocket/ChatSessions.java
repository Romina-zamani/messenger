package dev.mahdi.chat.websocket;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

/** Associates live sockets with the HTTP session that authenticated them. */
@Component
public class ChatSessions implements HandshakeInterceptor, HttpSessionListener {
    private static final String HTTP_SESSION = "chat.httpSession";
    private final Map<HttpSession, Set<WebSocketSession>> sockets = new ConcurrentHashMap<>();

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servlet)) return false;
        HttpSession session = servlet.getServletRequest().getSession(false);
        if (session == null || servlet.getPrincipal() == null) return false;
        attributes.put(HTTP_SESSION, session);
        return true;
    }

    @Override public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                         WebSocketHandler handler, Exception error) {}

    public boolean attach(WebSocketSession socket) {
        HttpSession session = (HttpSession) socket.getAttributes().get(HTTP_SESSION);
        if (session == null) return false;
        sockets.compute(session, (key, connections) -> {
            if (connections == null) connections = ConcurrentHashMap.newKeySet();
            connections.add(socket);
            return connections;
        });
        if (valid(socket)) return true;
        detach(socket);
        return false;
    }

    public boolean valid(WebSocketSession socket) {
        try {
            HttpSession session = (HttpSession) socket.getAttributes().get(HTTP_SESSION);
            if (session == null || socket.getPrincipal() == null) return false;
            if (session.getMaxInactiveInterval() > 0 && System.currentTimeMillis() - session.getLastAccessedTime()
                    >= session.getMaxInactiveInterval() * 1000L) return false;
            Object stored = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            return stored instanceof SecurityContext context && context.getAuthentication() != null
                && context.getAuthentication().isAuthenticated()
                && context.getAuthentication().getName().equals(socket.getPrincipal().getName());
        } catch (IllegalStateException expired) { return false; }
    }

    public void detach(WebSocketSession socket) {
        HttpSession session = (HttpSession) socket.getAttributes().get(HTTP_SESSION);
        if (session != null) sockets.computeIfPresent(session, (key, connections) -> {
            connections.remove(socket);
            return connections.isEmpty() ? null : connections;
        });
    }

    @Override public void sessionDestroyed(HttpSessionEvent event) {
        Set<WebSocketSession> connections = sockets.remove(event.getSession());
        if (connections != null) connections.forEach(ChatSessions::close);
    }

    public static void close(WebSocketSession socket) {
        try { socket.close(CloseStatus.POLICY_VIOLATION.withReason("Please sign in again.")); }
        catch (java.io.IOException ignored) { /* The connection is already gone. */ }
    }
}
