package my.chat.webchat.controller;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.Map;

@Controller
public class WebSocketController {
    private Map<String, String> onlineUsers;
    private SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.onlineUsers = new HashMap<>();
    }

    public ChatMessage sendMessage(ChatMessage message, SimpMessageHeaderAccessor headerAccessor){
        if (message.getContent().startsWith("/")){
            return handleCommand(message, headerAccessor);
        }

        return message;
    }

    /*
    добавил бы вывод всех команд, когда пользователь заходит в онлайн
    + создать отдельный допустимый функционал для "Admin"
    */
    public ChatMessage handleCommand(ChatMessage message, SimpMessageHeaderAccessor headerAccessor){
        switch (message.getContent()){
            case "/help" -> {
                return new ChatMessage("Server", "Доступные команды:\n/help\n/onlineUsers\n/privateMessage");
            }
            case "/onlineUsers" -> {
                return new ChatMessage();
            }
            // default -> ; --- придумаем
        }
    }

    // что именно приватного?
    public ChatMessage sendPrivateMessage(ChatMessage message, SimpMessagingTemplate messagingTemplate){

    }

    // возвращаемый тип данных не подходит, мб вернуть Map<String, String>?
    public ChatMessage onlineUser(ChatMessage message, SimpMessageHeaderAccessor headerAccessor){
        onlineUsers.put(headerAccessor.getSessionId(), message.getSender());
        return new ChatMessage("server, " + message + message.getSender());
    }

    public Map<String, String> getOnlineUsers() {
        return onlineUsers;
    }

    public void setOnlineUsers(Map<String, String> onlineUsers) {
        this.onlineUsers = onlineUsers;
    }

    public SimpMessagingTemplate getMessagingTemplate() {
        return messagingTemplate;
    }

    public void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    private static class ChatMessage {
        private String sender;
        private String receiver;
        private String type;
        private String content;

        public ChatMessage() {
        }

        public ChatMessage(String sender, String receiver, String type, String content) {
            this.sender = sender;
            this.receiver = receiver;
            this.type = type;
            this.content = content;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getReceiver() {
            return receiver;
        }

        public void setReceiver(String receiver) {
            this.receiver = receiver;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}