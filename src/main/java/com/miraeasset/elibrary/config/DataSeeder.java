package com.miraeasset.elibrary.config;

import com.miraeasset.elibrary.book.Book;
import com.miraeasset.elibrary.book.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final BookRepository bookRepository;

    @Value("${app.seed.bulk.enabled:false}")
    private boolean bulkEnabled;

    @Value("${app.seed.bulk.count:1000}")
    private int bulkCount;

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedBooks() {
        return args -> {
            if (bookRepository.count() > 0) {
                return;
            }
            bookRepository.saveAll(curatedBooks());
            if (bulkEnabled) {
                bookRepository.saveAll(generateBulk(bulkCount));
            }
        };
    }

    private List<Book> curatedBooks() {
        return List.of(
                Book.create("9780132350884", "Clean Code", "Robert C. Martin",
                        "A practical guide to writing readable and maintainable code.",
                        "Software Engineering", 2),
                Book.create("9781617294945", "Spring in Action", "Craig Walls",
                        "A guide to building applications with Spring.",
                        "Software Engineering", 1),
                Book.create("9780321356680", "Effective Java", "Joshua Bloch",
                        "Best practices for the Java programming language.",
                        "Programming", 3));
    }

    /**
     * Generates {@code count} plausible books with unique ISBNs so the browse
     * / pagination paths can be exercised against a realistically sized catalog.
     * Only used for measurement; disabled by default.
     */
    private List<Book> generateBulk(int count) {
        int size = Math.max(count, 0);
        List<Book> books = new ArrayList<>(size);

        String[] titles = {
                "Clean Code", "Functional Design", "Distributed Systems", "Cloud Native Patterns",
                "Data Engineering", "API Design Guide", "Concurrency in Depth", "Observability at Scale",
                "Secure Software", "Event-Driven Architecture", "Microservices in Practice", "Testing Strategies"};
        String[] authors = {
                "Ada Lovelace", "Grace Hopper", "Linus Torvalds", "Barbara Liskov",
                "Alan Turing", "Edsger Dijkstra", "Niklaus Wirth", "Ken Thompson"};
        String[] cats = {
                "Software Engineering", "Programming", "Architecture", "DevOps",
                "Data Science", "Security", "Testing", "Distributed Systems"};

        for (int i = 0; i < size; i++) {
            String isbn = String.format("9%016d", i);
            String title = titles[i % titles.length] + " Vol. " + (i / titles.length + 1);
            String author = authors[i % authors.length];
            String category = cats[i % cats.length];
            int licenses = 1 + (i % 5);
            books.add(Book.create(isbn, title, author,
                    "A deliberately generated description to give browse and pagination "
                            + "tests a realistic amount of text, used for performance baselining.",
                    category, licenses));
        }
        return books;
    }
}
