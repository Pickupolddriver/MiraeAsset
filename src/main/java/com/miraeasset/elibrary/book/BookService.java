package com.miraeasset.elibrary.book;

import com.miraeasset.elibrary.book.dto.BookDetailResponse;
import com.miraeasset.elibrary.book.dto.BookSummaryResponse;
import com.miraeasset.elibrary.common.InvalidRequestException;
import com.miraeasset.elibrary.common.ResourceNotFoundException;
import com.miraeasset.elibrary.common.dto.PageResponse;
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
    public PageResponse<BookSummaryResponse> list(String query, String category, int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("INVALID_PAGE", "page must not be negative");
        }
        if (size < 1 || size > 50) {
            throw new InvalidRequestException("INVALID_PAGE_SIZE", "size must be between 1 and 50");
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim();
        Page<Book> books = bookRepository.search(normalizedQuery, normalizedCategory, pageRequest);
        return PageResponse.from(books.map(BookSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public BookDetailResponse get(Long bookId) {
        return bookRepository.findById(bookId)
                .map(BookDetailResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("BOOK_NOT_FOUND", "Book does not exist"));
    }
}