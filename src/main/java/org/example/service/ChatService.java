package org.example.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.example.model.ChatMessage;
import org.example.model.ChatRequest;
import org.example.model.ChatResponse;
import org.example.model.ChatSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final EmbeddingIndexService index;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final int topK;
    private final int maxHistoryTurns;
    private final ChatModel chatModel;

    public ChatService(EmbeddingIndexService index,
                       @Value("${openrouter.api.key:}") String apiKey,
                       @Value("${openrouter.model}") String model,
                       @Value("${openrouter.base-url}") String baseUrl,
                       @Value("${chatbot.retrieval.top-k:4}") int topK,
                       @Value("${chatbot.history.max-turns:6}") int maxHistoryTurns) {
        this.index = index;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.topK = topK;
        this.maxHistoryTurns = maxHistoryTurns;
        this.chatModel = (apiKey == null || apiKey.isBlank()) ? null :
            OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .timeout(Duration.ofSeconds(60))
                .temperature(0.2)
                .build();
    }

    public ChatResponse chat(ChatRequest req) {
        if (chatModel == null) {
            return new ChatResponse(
                "Chatbot not configured: set OPENROUTER_API_KEY.", List.of());
        }

        List<EmbeddingMatch<TextSegment>> retrieved = index.findRelevant(req.message(), topK);
        List<ChatSource> sources = new ArrayList<>();
        StringBuilder ctx = new StringBuilder();
        for (EmbeddingMatch<TextSegment> m : retrieved) {
            ctx.append("- ").append(m.embedded().text()).append("\n");
            var md = m.embedded().metadata();
            sources.add(new ChatSource(
                md.getString("type"),
                md.getString("id"),
                md.getString("title") != null ? md.getString("title") : md.getString("name")));
        }

        String userDesc = req.user() != null ? index.userDescription(req.user())
                                             : "No specific user.";

        String system = """
            You are a book recommendation assistant.
            Answer ONLY using facts from the CONTEXT below.
            If the answer is not in the context, say you don't know.
            Do not invent authors, genres, or books.
            Current user: %s

            CONTEXT:
            %s
            """.formatted(userDesc, ctx.toString());

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(system));
        if (req.history() != null) {
            int start = Math.max(0, req.history().size() - maxHistoryTurns * 2);
            for (int i = start; i < req.history().size(); i++) {
                ChatMessage h = req.history().get(i);
                if ("assistant".equalsIgnoreCase(h.role())) {
                    messages.add(AiMessage.from(h.content()));
                } else {
                    messages.add(UserMessage.from(h.content()));
                }
            }
        }
        messages.add(UserMessage.from(req.message()));

        try {
            String reply = chatModel.chat(messages).aiMessage().text();
            return new ChatResponse(reply, sources);
        } catch (Exception e) {
            log.error("OpenRouter call failed", e);
            return new ChatResponse(
                "Sorry — the model is unavailable right now.", sources);
        }
    }
}
