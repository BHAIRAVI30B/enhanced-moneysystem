package com.example.backend.security.websocket;

import com.example.backend.security.jwt.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SessionWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(SessionWebSocketHandler.class);

    private final JwtUtils jwtUtils;

    // Maps username -> all open WebSocket sessions for that user
    // CopyOnWriteArrayList handles concurrent reads safely
    private final Map<String, CopyOnWriteArrayList<WebSocketSession>> userSessions
            = new ConcurrentHashMap<>();

    public SessionWebSocketHandler(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String username = extractUsername(session);
        if (username == null) {
            logger.warn("WebSocket connection rejected — no valid JWT in query param");
            closeQuietly(session);
            return;
        }

        userSessions.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>()).add(session);
        logger.info("WebSocket connected: user='{}', sessionId='{}'", username, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String username = extractUsername(session);
        if (username != null) {
            CopyOnWriteArrayList<WebSocketSession> sessions = userSessions.get(username);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(username);
                }
            }
        }
        logger.info("WebSocket disconnected: sessionId='{}', status='{}'", session.getId(), status);
    }

    /**
     * Called from AuthController on new login.
     * Sends a "SESSION_KICKED" message to all OTHER open sessions of this user,
     * then closes them. The new session is not registered yet at this point.
     */
    public void kickOtherSessions(String username) {
        CopyOnWriteArrayList<WebSocketSession> sessions = userSessions.get(username);
        if (sessions == null || sessions.isEmpty()) return;

        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage("SESSION_KICKED"));
                    session.close(CloseStatus.NORMAL);
                    logger.info("Kicked WebSocket session '{}' for user '{}'", session.getId(), username);
                }
            } catch (IOException e) {
                logger.error("Error kicking session '{}': {}", session.getId(), e.getMessage());
            }
        }
        userSessions.remove(username);
    }

    /**
     * Extracts the JWT from the WebSocket URL query param ?token=xxx
     * and returns the username if valid, null otherwise.
     */
    private String extractUsername(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) return null;

        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                String token = param.substring("token=".length());
                if (jwtUtils.validateJwtToken(token)) {
                    return jwtUtils.getUserNameFromJwtToken(token);
                }
            }
        }
        return null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.NOT_ACCEPTABLE);
        } catch (IOException e) {
            logger.error("Error closing session: {}", e.getMessage());
        }
    }
}
