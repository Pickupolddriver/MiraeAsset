package com.miraeasset.elibrary.book.dto;

import com.miraeasset.elibrary.book.Book;

public record BookSummaryResponse(
        Long id,
        String isbn,
        String title,
        String author,
        String category,
        int totalLicenses,
        int availableLicenses) {

    public static BookSummaryResponse from(Book book) {
        return new BookSummaryResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getCategory(), book.getTotalLicenses(), book.getAvailableLicenses());
    }
}
