package my.chat.webchat.controller;

import my.chat.webchat.pojo.ChatMessage;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebSocketController {
    private final Map<String, String> sessionToUser = new ConcurrentHashMap<>();
    private final Map<String, String> userToSession = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(ChatMessage message) {
        return message;
    }

    @MessageMapping("/chat.private")
    public void sendPrivateMessage(ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String senderSessionId = headerAccessor.getSessionId();
            String receiverSessionId = userToSession.get(message.getReceiver());

            if (receiverSessionId == null || receiverSessionId.isEmpty()) {
                sendError(senderSessionId, "Сессия получателя не указана");
                return;
            }

            if (!sessionToUser.containsKey(receiverSessionId)) {
                sendError(senderSessionId, "Сессия получателя не найдена");
                return;
            }

            if (senderSessionId.equals(receiverSessionId)) {
                sendError(senderSessionId, "Нельзя отправлять сообщения самому себе");
                return;
            }

            messagingTemplate.convertAndSendToUser(senderSessionId,
                    "/queue/private",
                    message,
                    createHeaders(senderSessionId));

            messagingTemplate.convertAndSendToUser(receiverSessionId,
                    "/queue/private",
                    message,
                    createHeaders(receiverSessionId));

        } catch (Exception e) {
            System.out.println("Error sending private message: " + e.getMessage());
        }
    }

    private void sendError(String sessionId, String error) {
        try {
            if (sessionId != null && sessionToUser.containsKey(sessionId)) {
                ChatMessage errorMessage = new ChatMessage("SERVER", error, "ERROR");
                messagingTemplate.convertAndSendToUser(sessionId, "/queue/errors", errorMessage);

                System.out.println("Error sent to session ID: "
                        + sessionId
                        + ". Error: "
                        + error);
            } else {
                System.out.println("Session ID is null or not found in sessionToUser map.");
            }
        } catch (Exception e) {
            System.out.println("Error sending error message: " + e.getMessage());
        }
    }

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        if (message.getSender() == null || message.getSender().trim().isEmpty()) {

            System.out.println("Username is empty");

            return new ChatMessage("SERVER", "Имя пользователя не может быть пустым", "ERROR");
        }

        String sessionId = headerAccessor.getSessionId();
        sessionToUser.put(sessionId, message.getSender());
        userToSession.put(message.getSender(), sessionId);

        System.out.println("User "
                + message.getSender()
                + " joined the chat with session ID: "
                + sessionId);

        return new ChatMessage("SERVER", message.getSender() + " присоединился к чату", "JOIN");
    }

    private MessageHeaders createHeaders(String sessionId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor
                .create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();

    }
}