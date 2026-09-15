package com.miraeasset.elibrary.config;

import com.miraeasset.elibrary.book.Book;
import com.miraeasset.elibrary.book.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final BookRepository bookRepository;

    @Bean
    @ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
    CommandLineRunner seedBooks() {
        return args -> {
            if (bookRepository.count() > 0) {
                return;
            }
            bookRepository.save(Book.create(
                    "9780132350884",
                    "Clean Code",
                    "Robert C. Martin",
                    "A practical guide to writing readable and maintainable code.",
                    "Software Engineering",
                    2));
            bookRepository.save(Book.create(
                    "9781617294945",
                    "Spring in Action",
                    "Craig Walls",
                    "A guide to building applications with Spring.",
                    "Software Engineering",
                    1));
            bookRepository.save(Book.create(
                    "9780321356680",
                    "Effective Java",
                    "Joshua Bloch",
                    "Best practices for the Java programming language.",
                    "Programming",
                    3));
        };
    }
}
