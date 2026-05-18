package org.example.controller;

import org.example.model.ChatRequest;
import org.example.model.ChatResponse;
import org.example.model.StartersResponse;
import org.example.service.ChatService;
import org.example.service.ChatStarterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatStarterService starterService;

    public ChatController(ChatService chatService, ChatStarterService starterService) {
        this.chatService = chatService;
        this.starterService = starterService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {
        return chatService.chat(req);
    }

    @GetMapping("/starters")
    public StartersResponse starters(@RequestParam(required = false) String page,
                                     @RequestParam(required = false) String id,
                                     @RequestParam(required = false) String user) {
        return starterService.starters(page, id, user);
    }
}
