package com.miraeasset.elibrary.book;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "books")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 120)
    private String author;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private int totalLicenses;

    @Column(nullable = false)
    private int availableLicenses;

    private Book(String isbn, String title, String author, String description, String category, int totalLicenses) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.description = description;
        this.category = category;
        this.totalLicenses = totalLicenses;
        this.availableLicenses = totalLicenses;
    }

    public static Book create(String isbn, String title, String author, String description,
                              String category, int totalLicenses) {
        if (totalLicenses < 1) {
            throw new IllegalArgumentException("totalLicenses must be greater than zero");
        }
        return new Book(isbn, title, author, description, category, totalLicenses);
    }

    public void borrowLicense() {
        if (availableLicenses <= 0) {
            throw new IllegalStateException("No license is available");
        }
        availableLicenses--;
    }

    public void returnLicense() {
        if (availableLicenses >= totalLicenses) {
            throw new IllegalStateException("All licenses are already available");
        }
        availableLicenses++;
    }
}
