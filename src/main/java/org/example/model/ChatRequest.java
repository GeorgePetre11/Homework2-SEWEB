package org.example.model;

import java.util.List;

public record ChatRequest(String user, String message,
                          List<ChatMessage> history,
                          PageContext page) {}
