package org.example.controller;

import org.example.service.BookService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/books")
    public String listBooks(Model model) {
        model.addAttribute("books", bookService.listBooks());
        return "books";
    }

    @GetMapping("/books/add")
    public String addBookForm(Model model) {
        model.addAttribute("genres", bookService.getAvailableGenres());
        model.addAttribute("levels", bookService.getAvailableLevels());
        return "book-add";
    }

    @PostMapping("/books/add")
    public String addBook(@RequestParam String id,
                          @RequestParam String title,
                          @RequestParam List<String> genres,
                          @RequestParam String readingLevel,
                          RedirectAttributes flash) {
        bookService.addBook(id, title, genres, readingLevel);
        flash.addFlashAttribute("message", "Book \"" + title + "\" added successfully.");
        return "redirect:/books";
    }

    @GetMapping("/books/edit/{id}")
    public String editBookForm(@PathVariable String id, Model model) {
        Map<String, String> book = bookService.getBook(id);
        if (book == null) return "redirect:/books";
        model.addAttribute("book", book);
        model.addAttribute("genres", bookService.getAvailableGenres());
        model.addAttribute("levels", bookService.getAvailableLevels());
        return "book-edit";
    }

    @PostMapping("/books/edit/{id}")
    public String editBook(@PathVariable String id,
                           @RequestParam List<String> genres,
                           @RequestParam String readingLevel,
                           RedirectAttributes flash) {
        bookService.updateBook(id, genres, readingLevel);
        flash.addFlashAttribute("message", "Book updated successfully.");
        return "redirect:/books";
    }
}
