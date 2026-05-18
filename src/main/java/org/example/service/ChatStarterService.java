package org.example.service;

import org.example.model.StartersResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChatStarterService {

    private final BookService bookService;

    public ChatStarterService(BookService bookService) {
        this.bookService = bookService;
    }

    public StartersResponse starters(String page, String id, String user) {
        String u = user != null && !user.isBlank() ? user : "the user";
        return switch (page == null ? "home" : page) {
            case "books" -> new StartersResponse(List.of(
                "Which book would " + u + " most likely enjoy?",
                "Which books match " + u + "'s reading level?",
                "Show me books matching " + u + "'s preferred genre."
            ));
            case "book-detail" -> {
                String title = id;
                if (id != null) {
                    Map<String, String> b = bookService.getBook(id);
                    if (b != null && b.get("label") != null) title = b.get("label");
                }
                yield new StartersResponse(List.of(
                    "Tell me about " + title + ".",
                    "Who wrote " + title + "?",
                    "Is " + title + " a good fit for " + u + "?"
                ));
            }
            default -> new StartersResponse(List.of(
                "What books are available?",
                "Recommend something for " + u + ".",
                "What genres do you have?"
            ));
        };
    }
}
