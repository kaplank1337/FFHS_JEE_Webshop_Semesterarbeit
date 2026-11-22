package com.jee.shop.shipping.messaging;

import com.jee.shop.shipping.security.CurrentUserService;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.StringReader;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Authenticated WebSocket endpoint owned by Shipping-Service for delivery updates. */
@ServerEndpoint("/ws/orders")
public class ShippingStatusWebSocketEndpoint {

    private static final String USER_ID_PROPERTY = ShippingStatusWebSocketEndpoint.class.getName() + ".userId";
    private static final ConcurrentHashMap<UUID, Set<Session>> SESSIONS_BY_USER = new ConcurrentHashMap<>();

    @OnMessage
    public void onMessage(String message, Session session) {
        if (session.getUserProperties().containsKey(USER_ID_PROPERTY)) {
            return;
        }

        try (JsonReader reader = Json.createReader(new StringReader(message))) {
            JsonObject payload = reader.readObject();
            String token = payload.getString("token", "");
            if (token.isBlank()) {
                closeUnauthorized(session, "A Bearer token is required");
                return;
            }

            UUID userId = CDI.current().select(CurrentUserService.class)
                    .get().requireUserId("Bearer " + token);
            session.getUserProperties().put(USER_ID_PROPERTY, userId);
            SESSIONS_BY_USER.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
            session.getBasicRemote().sendText("{\"type\":\"AUTHENTICATED\"}");
        } catch (Exception e) {
            closeUnauthorized(session, "The token could not be validated");
        }
    }

    @OnClose
    public void onClose(Session session) {
        removeSession(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        removeSession(session);
    }

    /** Pushes a persisted shipping state to connected sockets belonging to this order's owner. */
    public static void publishStatusChanged(UUID userId, UUID orderId, String status) {
        Set<Session> sessions = SESSIONS_BY_USER.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json = Json.createObjectBuilder()
                .add("type", "ORDER_STATUS_CHANGED")
                .add("orderId", orderId.toString())
                .add("status", status)
                .build().toString();

        for (Session session : sessions) {
            if (!session.isOpen()) {
                removeSession(session);
                continue;
            }
            session.getAsyncRemote().sendText(json, result -> {
                if (!result.isOK()) {
                    removeSession(session);
                }
            });
        }
    }

    private static void closeUnauthorized(Session session, String reason) {
        try {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, reason));
        } catch (Exception ignored) {
            // The peer may already have disconnected.
        }
    }

    private static void removeSession(Session session) {
        Object userId = session.getUserProperties().remove(USER_ID_PROPERTY);
        if (userId instanceof UUID id) {
            SESSIONS_BY_USER.computeIfPresent(id, (ignored, sessions) -> {
                sessions.remove(session);
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }
}
