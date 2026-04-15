package com.luisa.wechat.chat.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luisa.wechat.chat.config.handler.ChatWebSocketHandler;
import com.luisa.wechat.chat.model.dto.MessageView;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ChatWebSocketNotifier {

    private final ChatWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    public ChatWebSocketNotifier(ChatWebSocketHandler webSocketHandler, ObjectMapper objectMapper) {
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    public void notifyUsers(Set<String> usernames) {
        try {
            webSocketHandler.notifyUsers(usernames, objectMapper.writeValueAsString(new RefreshEvent("refresh")));
        } catch (JsonProcessingException ignored) {
        }
    }

    public void notifyLobbyMessage(Set<String> usernames, MessageView message) {
        notifyUsers(usernames, new MessageEvent("message", "LOBBY", null, null, null, message));
    }

    public void notifyDirectMessage(Set<String> usernames,
                                    String senderUsername,
                                    String targetUsername,
                                    MessageView message) {
        notifyUsers(usernames, new MessageEvent("message", "DIRECT", senderUsername, targetUsername, null, message));
    }

    public void notifyGroupMessage(Set<String> usernames, Long groupId, MessageView message) {
        notifyUsers(usernames, new MessageEvent("message", "GROUP", null, null, groupId, message));
    }

    private void notifyUsers(Set<String> usernames, Object event) {
        try {
            webSocketHandler.notifyUsers(usernames, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ignored) {
        }
    }

    public record RefreshEvent(String type) {
    }

    public record MessageEvent(String type,
                               String scope,
                               String senderUsername,
                               String targetUsername,
                               Long groupId,
                               MessageView message) {
    }
}
