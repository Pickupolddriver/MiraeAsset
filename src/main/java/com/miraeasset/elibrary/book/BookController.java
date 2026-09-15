package com.miraeasset.elibrary.book;

import com.miraeasset.elibrary.book.dto.BookDetailResponse;
import com.miraeasset.elibrary.book.dto.BookSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "Browse books")
    public Page<BookSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookService.list(q, page, size);
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details")
    public BookDetailResponse get(
            @Parameter(description = "Book identifier") @PathVariable Long bookId) {
        return bookService.get(bookId);
    }
}
