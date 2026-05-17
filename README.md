# Semantic Web — Homework 2

Book recommendation system built around RDF/XML, OWL ontologies, SPARQL, and a RAG-powered chatbot.

## Team

- Petre George-Alexandru
- Ionescu Rares-Andrei

## Link to GitHub

https://github.com/GeorgePetre11/Homework2-SEWEB

## Task 1 — George

> Write a RDF/XML for the following scenario (1 pt)

Modeled the book recommendation scenario in `books.rdf` with RDFS classes (Book, User, Genre, ReadingLevel), properties (hasGenre, hasReadingLevel, prefersGenre, isRecommendedTo), and all the data from the assignment: users Alice and Bob with their reading levels and preferred genres, and books Dune, The Silent Patient, and Hunger Games with their genres and reading levels.

## Task 2 — George

> Add a feature to your web application that allows the user to upload a RDF/XML file and visualize its RDF graph. You may use Jung or a similar API. Test the feature with the file created at point 1. (1 pt)

Spring Boot 3 + Thymeleaf webapp with Apache Jena for RDF parsing. The upload page (`/upload`) accepts an RDF/XML file via drag-and-drop or file picker, parses it with `RdfService` using Jena's `ModelFactory`, and returns all triples as JSON. The frontend renders an interactive force-directed graph using **vis.js** (nodes for subjects/objects, labeled directed edges for predicates) and a triples table below the graph. Tested with the `books.rdf` file from Task 1.

## Task 3 — George

> In the web application, add a feature to let you modify or add a book. Test the features for the book "Harry Potter" (add book) and "Hunger Games" (change reading level). You must use RDF and JENA API / other RDF API in order to write, read, query and perform operations. (0.5 pt)

`BookService` manages the Jena `Model` in memory (loaded from `books.rdf` at startup). Adding a book creates a new `book:Book` resource with genre and reading level properties; editing removes the old property statements and writes new ones. Both operations persist changes back to `books.rdf` via Jena's `model.write()`. The add form (`/books/add`) and edit form (`/books/edit/{id}`) use Thymeleaf with dropdowns for genres (multi-select) and reading level. Tested by adding "Harry Potter" (Fantasy, Intermediate) and changing "Hunger Games" from Beginner to Intermediate.

## Task 4 — George

> In the web application, list all the available books and provide a dedicated page for each book. You have to use RDF and JENA API / other RDF API in order to write, read, query and perform operations. (0.5pt for listing, 0.5pt for book info page)

The `/books` page lists all books from the Jena model in a table with clickable titles, genres, and reading levels. Each title links to a dedicated detail page at `/books/{id}` showing the book's RDF URI, genres as badges, reading level, and an edit button. All data is read from the in-memory Jena `Model` via `BookService`.

## Task 5

> Create an OWL ontology for the book recommendation system presented in the first exercise. (1.5 pt)

### Rares

- OWL ontology: `ontology/book-recommendation.owl`
- Screenshots/exported graphs: `docs/screenshots/`

## Task 6

> Make 5 SPARQL queries for your ontology and save them in a txt file with the name "sparql_owl". (1 pt)

### Rares

- Queries: `sparql_owl`
- Screenshots of query results: `docs/screenshots/ex6/`

## Task 7

> Build a chatbot that allows the user to perform operations: floating chat window, context-aware conversation starters, RAG-enhanced responses, and book search by theme/author. (4 pt)

### Rares
