package com.miraeasset.elibrary.book.dto;

import com.miraeasset.elibrary.book.Book;

public record BookDetailResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String description,
        String category,
        int totalLicenses,
        int availableLicenses) {

    public static BookDetailResponse from(Book book) {
        return new BookDetailResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getDescription(), book.getCategory(), book.getTotalLicenses(), book.getAvailableLicenses());
    }
}
