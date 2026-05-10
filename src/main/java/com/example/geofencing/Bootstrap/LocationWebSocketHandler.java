package com.example.geofencing.Bootstrap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.example.geofencing.Services.JwtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class LocationWebSocketHandler extends TextWebSocketHandler {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> adminSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null || !jwtService.validateToken(token)) {
            closeSession(session, CloseStatus.NOT_ACCEPTABLE.withReason("Invalid or missing token"));
            return;
        }

        try {
            var claims = jwtService.extractAllClaims(token);
            String userId = claims.getSubject();

            // roles are stored as a List in the JWT, e.g. ["ADMIN"] or ["USER"]
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles");
            String role = (roles != null && !roles.isEmpty()) ? roles.get(0) : null;

            session.getAttributes().put("userId", userId);
            session.getAttributes().put("role", role);

            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
            sessionUserMap.put(session.getId(), userId);

            if ("ADMIN".equals(role) || "OPERATOR".equals(role)) {
                adminSessions.add(session);
            }

            sendToSession(session, Map.of("type", "connected", "userId", userId, "message", "WebSocket connected"));
            log.info("WebSocket connected: user={} sessionId={}", userId, session.getId());

        } catch (Exception e) {
            log.error("Error establishing WebSocket connection: {}", e.getMessage());
            closeSession(session, CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            var payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            switch (type != null ? type : "") {
                case "ping" -> sendToSession(session, Map.of("type", "pong", "timestamp", System.currentTimeMillis()));
                case "subscribe_geofence" -> {
                    String geofenceId = (String) payload.get("geofenceId");
                    if (geofenceId != null) {
                        session.getAttributes().put("subscribed_geofence", geofenceId);
                        sendToSession(session, Map.of("type", "subscribed", "geofenceId", geofenceId));
                    }
                }
                default -> log.debug("Unknown WebSocket message type: {}", type);
            }
        } catch (JsonProcessingException e) {
            log.warn("Error handling WebSocket message: {}", e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = sessionUserMap.remove(session.getId());
        if (userId != null) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
        adminSessions.remove(session);
        log.info("WebSocket disconnected: sessionId={} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        afterConnectionClosed(session, CloseStatus.SERVER_ERROR);
    }

    public void sendLocationUpdate(String userId, Object locationDto) {
        sendToUser(userId, Map.of("type", "location_update", "data", locationDto));
    }

    public void sendGeofenceEvent(String userId, Object event) {
        sendToUser(userId, Map.of("type", "geofence_event", "data", event));
    }

    public void sendAlertToUser(String userId, Object alert) {
        sendToUser(userId, Map.of("type", "alert", "data", alert));
    }

    public void broadcastToAdmins(String type, Object data) {
        Map<String, Object> msg = Map.of("type", type, "data", data);
        for (WebSocketSession session : adminSessions) {
            sendToSession(session, msg);
        }
    }

    public int getConnectedUsersCount() {
        return userSessions.size();
    }

    public boolean isUserConnected(String userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }
    
    private void sendToUser(String userId, Object message) {
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) return;
        for (WebSocketSession session : sessions) {
            sendToSession(session, message);
        }
    }

    private void sendToSession(WebSocketSession session, Object message) {
        if (!session.isOpen()) return;
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            }
        } catch (IOException e) {
            log.warn("Failed to send WebSocket message to session {}: {}", session.getId(), e.getMessage());
        }
    }

    private String extractToken(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) return param.substring(6);
            }
        }
        List<String> authHeaders = session.getHandshakeHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) return header.substring(7);
        }
        return null;
    }

    private void closeSession(WebSocketSession session, CloseStatus status) {
        try { session.close(status); } catch (IOException ignored) {}
    }
}