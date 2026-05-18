package org.example.model;

import java.util.List;

public record ChatResponse(String reply, List<ChatSource> sources) {}
