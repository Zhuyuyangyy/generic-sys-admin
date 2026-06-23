package com.zyy.websocket;

import com.zyy.util.JsonUtil;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * WebSocket endpoint for real-time event push.
 * <p>
 * Connected clients receive JSON-formatted events via the
 * {@code /ws/events} channel. Supports both broadcast and
 * user-targeted delivery.
 * <p>
 * Event format:
 * <pre>{@code
 * {
 *   "type": "STOCK_ALERT",
 *   "data": { ... },
 *   "timestamp": "2026-06-23T10:00:00"
 * }
 * }</pre>
 *
 * @author System Architect
 */
@Slf4j
@Component
@ServerEndpoint("/ws/events")
public class EventWebSocket {

    /** All active sessions keyed by session ID */
    private static final ConcurrentHashMap<String, Session> SESSIONS = new ConcurrentHashMap<>();

    /** User ID → list of session IDs (a user may have multiple tabs) */
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<String>> USER_SESSIONS = new ConcurrentHashMap<>();

    /** In-memory buffer of recent events (last 50) */
    private static final CopyOnWriteArrayList<Event> RECENT_EVENTS = new CopyOnWriteArrayList<>();

    private static final int MAX_RECENT_EVENTS = 50;

    // ==================== Lifecycle Callbacks ====================

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.put(session.getId(), session);

        // Try to extract userId from query params
        Map<String, List<String>> params = session.getRequestParameterMap();
        List<String> userIdParams = params.getOrDefault("userId", Collections.emptyList());
        if (!userIdParams.isEmpty()) {
            try {
                Long userId = Long.parseLong(userIdParams.get(0));
                USER_SESSIONS.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session.getId());
            } catch (NumberFormatException e) {
                log.warn("Invalid userId parameter in WebSocket connection: {}", userIdParams.get(0));
            }
        }

        // Send welcome message
        Event welcome = new Event();
        welcome.setType("CONNECTED");
        welcome.setData("WebSocket connection established");
        welcome.setTimestamp(LocalDateTime.now());
        sendToSession(session, welcome);

        log.info("WebSocket session opened - sessionId={}, totalSessions={}", session.getId(), SESSIONS.size());
    }

    @OnClose
    public void onClose(Session session) {
        SESSIONS.remove(session.getId());
        // Remove from user mapping
        USER_SESSIONS.values().forEach(list -> list.remove(session.getId()));
        log.info("WebSocket session closed - sessionId={}, totalSessions={}", session.getId(), SESSIONS.size());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if ("ping".equalsIgnoreCase(message)) {
            sendToSession(session, createEvent("PONG", null));
            return;
        }

        // Subscription-style messages can be handled here in the future
        log.debug("WebSocket message received - sessionId={}, message={}", session.getId(), message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        log.error("WebSocket error - sessionId={}, error={}", session.getId(), throwable.getMessage());
        if (session != null && session.isOpen()) {
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Server error"));
            } catch (IOException e) {
                log.warn("Failed to close WebSocket session on error: {}", e.getMessage());
            }
        }
    }

    // ==================== Static Broadcast Methods ====================

    /**
     * Broadcast an event to all connected sessions.
     *
     * @param type Event type identifier
     * @param data Event payload
     */
    public static void broadcastEvent(String type, Object data) {
        Event event = createEvent(type, data);
        addToRecent(event);

        String json = JsonUtil.toJson(event);
        for (Session session : SESSIONS.values()) {
            if (session.isOpen()) {
                session.getAsyncRemote().sendText(json);
            }
        }

        log.info("Event broadcast - type={}, sessions={}", type, SESSIONS.size());
    }

    /**
     * Send an event to a specific user.
     *
     * @param userId Target user ID
     * @param type   Event type identifier
     * @param data   Event payload
     */
    public static void sendToUser(Long userId, String type, Object data) {
        Event event = createEvent(type, data);
        event.setUserId(userId);
        addToRecent(event);

        CopyOnWriteArrayList<String> sessionIds = USER_SESSIONS.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            log.debug("No active WebSocket sessions for userId={}", userId);
            return;
        }

        String json = JsonUtil.toJson(event);
        for (String sessionId : sessionIds) {
            Session session = SESSIONS.get(sessionId);
            if (session != null && session.isOpen()) {
                session.getAsyncRemote().sendText(json);
            }
        }

        log.info("Event sent to user - userId={}, type={}", userId, type);
    }

    /**
     * Get the list of recent events (last 50).
     *
     * @return Unmodifiable list of recent events
     */
    public static List<Event> getRecentEvents() {
        return Collections.unmodifiableList(RECENT_EVENTS);
    }

    /**
     * Get WebSocket connection info.
     *
     * @return Map with connection statistics
     */
    public static Map<String, Object> getConnectionInfo() {
        return Map.of(
                "totalSessions", SESSIONS.size(),
                "connectedUsers", USER_SESSIONS.size(),
                "recentEventCount", RECENT_EVENTS.size()
        );
    }

    // ==================== Private Helpers ====================

    private static Event createEvent(String type, Object data) {
        Event event = new Event();
        event.setType(type);
        event.setData(data);
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    private static void sendToSession(Session session, Event event) {
        if (session.isOpen()) {
            session.getAsyncRemote().sendText(JsonUtil.toJson(event));
        }
    }

    private static void addToRecent(Event event) {
        RECENT_EVENTS.add(event);
        while (RECENT_EVENTS.size() > MAX_RECENT_EVENTS) {
            RECENT_EVENTS.remove(0);
        }
    }
}
