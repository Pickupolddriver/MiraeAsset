package com.miraeasset.elibrary.book;

import com.miraeasset.elibrary.common.ResourceNotFoundException;
import com.miraeasset.elibrary.book.dto.BookDetailResponse;
import com.miraeasset.elibrary.book.dto.BookSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public Page<BookSummaryResponse> list(String query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by("id").ascending());
        Page<Book> books = query == null || query.isBlank()
                ? bookRepository.findAll(pageRequest)
                : bookRepository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(
                        query.trim(), query.trim(), pageRequest);
        return books.map(BookSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public BookDetailResponse get(Long bookId) {
        return bookRepository.findById(bookId)
                .map(BookDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("BOOK_NOT_FOUND", "Book does not exist"));
    }
}
