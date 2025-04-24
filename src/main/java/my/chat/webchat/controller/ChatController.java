package my.chat.webchat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    public ChatController() {

    }

    @GetMapping("/")
    public String chat(){
        return "chat";
    }
}
