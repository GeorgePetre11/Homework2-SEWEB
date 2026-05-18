package org.example.service;

import jakarta.annotation.PostConstruct;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class BookService {

    private static final String BOOK_NS = "http://example.org/book#";
    private static final String USER_NS = "http://example.org/user#";
    private static final String RDF_FILE = "books.rdf";
    private static final String HAS_AUTHOR = BOOK_NS + "hasAuthor";

    private Model model;
    private final ObjectProvider<EmbeddingIndexService> indexProvider;

    public BookService(ObjectProvider<EmbeddingIndexService> indexProvider) {
        this.indexProvider = indexProvider;
    }

    @PostConstruct
    public void init() {
        model = ModelFactory.createDefaultModel();
        File file = new File(RDF_FILE);
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                model.read(in, null, "RDF/XML");
            } catch (IOException e) {
                throw new RuntimeException("Failed to load " + RDF_FILE, e);
            }
        }
    }

    public synchronized List<Map<String, String>> listBooks() {
        List<Map<String, String>> books = new ArrayList<>();
        Resource bookClass = model.getResource(BOOK_NS + "Book");
        Property hasGenre = model.getProperty(BOOK_NS + "hasGenre");
        Property hasReadingLevel = model.getProperty(BOOK_NS + "hasReadingLevel");
        Property hasAuthor = model.getProperty(HAS_AUTHOR);

        ResIterator it = model.listResourcesWithProperty(
                model.getProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), bookClass);

        while (it.hasNext()) {
            Resource book = it.next();
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("uri", book.getURI());
            entry.put("id", book.getURI().substring(BOOK_NS.length()));

            Statement labelStmt = book.getProperty(RDFS.label);
            entry.put("label", labelStmt != null ? labelStmt.getString() : book.getLocalName());

            List<String> genres = new ArrayList<>();
            StmtIterator genreIt = book.listProperties(hasGenre);
            while (genreIt.hasNext()) {
                Resource genre = genreIt.next().getResource();
                Statement gl = genre.getProperty(RDFS.label);
                genres.add(gl != null ? gl.getString() : genre.getLocalName());
            }
            entry.put("genres", String.join(", ", genres));

            Statement levelStmt = book.getProperty(hasReadingLevel);
            if (levelStmt != null) {
                Resource level = levelStmt.getResource();
                Statement ll = level.getProperty(RDFS.label);
                entry.put("readingLevel", ll != null ? ll.getString() : level.getLocalName());
            }

            Statement authorStmt = book.getProperty(hasAuthor);
            entry.put("author", authorStmt != null ? authorStmt.getString() : "");

            books.add(entry);
        }
        return books;
    }

    public synchronized void addBook(String id, String title, String author,
                                     List<String> genres, String readingLevel) {
        Resource bookClass = model.getResource(BOOK_NS + "Book");
        Property hasGenre = model.getProperty(BOOK_NS + "hasGenre");
        Property hasReadingLevel = model.getProperty(BOOK_NS + "hasReadingLevel");
        Property hasAuthor = model.getProperty(HAS_AUTHOR);

        Resource book = model.createResource(BOOK_NS + id, bookClass);
        book.addProperty(RDFS.label, title);
        if (author != null && !author.isBlank()) {
            book.addLiteral(hasAuthor, author);
        }

        for (String genre : genres) {
            Resource genreRes = model.getResource(BOOK_NS + genre);
            book.addProperty(hasGenre, genreRes);
        }

        Resource levelRes = model.getResource(BOOK_NS + readingLevel);
        book.addProperty(hasReadingLevel, levelRes);

        save();
        EmbeddingIndexService idx = indexProvider.getIfAvailable();
        if (idx != null) idx.rebuild();
    }

    public synchronized void updateBook(String id, String author,
                                        List<String> genres, String readingLevel) {
        Resource book = model.getResource(BOOK_NS + id);
        Property hasGenre = model.getProperty(BOOK_NS + "hasGenre");
        Property hasReadingLevel = model.getProperty(BOOK_NS + "hasReadingLevel");
        Property hasAuthor = model.getProperty(HAS_AUTHOR);

        if (author != null) {
            book.removeAll(hasAuthor);
            if (!author.isBlank()) {
                book.addLiteral(hasAuthor, author);
            }
        }

        if (genres != null) {
            book.removeAll(hasGenre);
            for (String genre : genres) {
                Resource genreRes = model.getResource(BOOK_NS + genre);
                book.addProperty(hasGenre, genreRes);
            }
        }

        if (readingLevel != null) {
            book.removeAll(hasReadingLevel);
            Resource levelRes = model.getResource(BOOK_NS + readingLevel);
            book.addProperty(hasReadingLevel, levelRes);
        }

        save();
        EmbeddingIndexService idx = indexProvider.getIfAvailable();
        if (idx != null) idx.rebuild();
    }

    public synchronized Map<String, String> getBook(String id) {
        Resource book = model.getResource(BOOK_NS + id);
        if (!model.containsResource(book)) return null;

        Property hasGenre = model.getProperty(BOOK_NS + "hasGenre");
        Property hasReadingLevel = model.getProperty(BOOK_NS + "hasReadingLevel");

        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("id", id);

        Statement labelStmt = book.getProperty(RDFS.label);
        entry.put("label", labelStmt != null ? labelStmt.getString() : id);

        List<String> genres = new ArrayList<>();
        StmtIterator genreIt = book.listProperties(hasGenre);
        while (genreIt.hasNext()) {
            Resource genre = genreIt.next().getResource();
            genres.add(genre.getURI().substring(BOOK_NS.length()));
        }
        entry.put("genres", String.join(",", genres));

        Statement levelStmt = book.getProperty(hasReadingLevel);
        if (levelStmt != null) {
            entry.put("readingLevel", levelStmt.getResource().getURI().substring(BOOK_NS.length()));
        }

        Property hasAuthor = model.getProperty(HAS_AUTHOR);
        Statement authorStmt = book.getProperty(hasAuthor);
        entry.put("author", authorStmt != null ? authorStmt.getString() : "");

        return entry;
    }

    public List<String> getAvailableGenres() {
        return List.of("ScienceFiction", "Fantasy", "Mystery", "Murder");
    }

    public List<String> getAvailableLevels() {
        return List.of("Beginner", "Intermediate", "Advanced");
    }

    public Model getModel() {
        return model;
    }

    private void save() {
        try (OutputStream out = new FileOutputStream(RDF_FILE)) {
            model.write(out, "RDF/XML");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save " + RDF_FILE, e);
        }
    }
}
