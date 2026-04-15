package com.luisa.wechat.chat.config.handler;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, Set<WebSocketSession>> sessionsByUsername = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal == null) {
            return;
        }
        sessionsByUsername.computeIfAbsent(principal.getName(), key -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Principal principal = session.getPrincipal();
        if (principal == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUsername.get(principal.getName());
        if (sessions != null) {
            sessions.remove(session);
        }
    }

    public void notifyUsers(Set<String> usernames, String payload) {
        for (String username : usernames) {
            Set<WebSocketSession> sessions = sessionsByUsername.get(username);
            if (sessions == null) {
                continue;
            }
            for (WebSocketSession session : sessions) {
                if (!session.isOpen()) {
                    continue;
                }
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException ignored) {
                }
            }
        }
    }
}
