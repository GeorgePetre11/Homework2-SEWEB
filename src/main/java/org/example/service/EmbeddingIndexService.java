package org.example.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmbeddingIndexService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexService.class);

    private static final String BOOK_NS = "http://example.org/book#";
    private static final String USER_NS = "http://example.org/user#";

    private final BookService bookService;
    private final EmbeddingModel embeddingModel;
    private final InMemoryEmbeddingStore<TextSegment> store;

    public EmbeddingIndexService(BookService bookService) {
        this.bookService = bookService;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        this.store = new InMemoryEmbeddingStore<>();
    }

    @PostConstruct
    public void init() {
        rebuild();
    }

    public synchronized void rebuild() {
        store.removeAll();
        int books = indexBooks();
        int users = indexUsers();
        log.info("Embedding index rebuilt: {} books, {} users", books, users);
    }

    private int indexBooks() {
        int count = 0;
        for (Map<String, String> b : bookService.listBooks()) {
            String text = String.format(
                "Title: %s. Author: %s. Genres: %s. Reading level: %s.",
                b.getOrDefault("label", ""),
                b.getOrDefault("author", ""),
                b.getOrDefault("genres", ""),
                b.getOrDefault("readingLevel", "")
            );
            Metadata md = new Metadata()
                .put("type", "book")
                .put("id", b.getOrDefault("id", ""))
                .put("title", b.getOrDefault("label", ""));
            TextSegment segment = TextSegment.from(text, md);
            Embedding embedding = embeddingModel.embed(segment).content();
            store.add(embedding, segment);
            count++;
        }
        return count;
    }

    private record UserFacts(String name, String level, String prefs) {}

    private UserFacts readUserFacts(Resource user, Property prefersGenre, Property hasReadingLevel) {
        Statement labelStmt = user.getProperty(RDFS.label);
        String name = labelStmt != null ? labelStmt.getString() : user.getLocalName();

        String level = "Unknown";
        Statement levelStmt = user.getProperty(hasReadingLevel);
        if (levelStmt != null) {
            Resource levelRes = levelStmt.getResource();
            Statement ll = levelRes.getProperty(RDFS.label);
            level = ll != null ? ll.getString() : levelRes.getLocalName();
        }

        List<String> prefs = new ArrayList<>();
        StmtIterator pit = user.listProperties(prefersGenre);
        while (pit.hasNext()) {
            Resource g = pit.next().getResource();
            Statement gl = g.getProperty(RDFS.label);
            prefs.add(gl != null ? gl.getString() : g.getLocalName());
        }
        return new UserFacts(name, level, String.join(", ", prefs));
    }

    private int indexUsers() {
        Model model = bookService.getModel();
        Property prefersGenre = model.getProperty(USER_NS + "prefersGenre");
        Property hasReadingLevel = model.getProperty(USER_NS + "hasReadingLevel");
        Resource userClass = model.getResource(USER_NS + "User");

        int count = 0;
        ResIterator it = model.listResourcesWithProperty(
            model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), userClass);
        while (it.hasNext()) {
            Resource user = it.next();
            UserFacts facts = readUserFacts(user, prefersGenre, hasReadingLevel);
            String text = String.format(
                "User: %s. Reading level: %s. Prefers: %s.",
                facts.name(), facts.level(), facts.prefs());
            Metadata md = new Metadata()
                .put("type", "user")
                .put("id", user.getLocalName())
                .put("name", facts.name());
            TextSegment segment = TextSegment.from(text, md);
            Embedding embedding = embeddingModel.embed(segment).content();
            store.add(embedding, segment);
            count++;
        }
        return count;
    }

    public synchronized List<EmbeddingMatch<TextSegment>> findRelevant(String query, int topK) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest req = EmbeddingSearchRequest.builder()
            .queryEmbedding(queryEmbedding)
            .maxResults(topK)
            .build();
        EmbeddingSearchResult<TextSegment> result = store.search(req);
        return result.matches();
    }

    public synchronized String userDescription(String userId) {
        Model model = bookService.getModel();
        Resource user = model.getResource(USER_NS + userId);
        if (!model.containsResource(user)) {
            return "User: " + userId + " (unknown).";
        }
        Property prefersGenre = model.getProperty(USER_NS + "prefersGenre");
        Property hasReadingLevel = model.getProperty(USER_NS + "hasReadingLevel");
        UserFacts facts = readUserFacts(user, prefersGenre, hasReadingLevel);
        return String.format("User %s prefers %s and has reading level %s.",
            facts.name(), facts.prefs(), facts.level());
    }
}
