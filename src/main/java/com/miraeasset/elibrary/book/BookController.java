package com.miraeasset.elibrary.book;

import com.miraeasset.elibrary.book.dto.BookDetailResponse;
import com.miraeasset.elibrary.book.dto.BookSummaryResponse;
import com.miraeasset.elibrary.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
    public PageResponse<BookSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookService.list(q, category, page, size);
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details")
    public BookDetailResponse get(
            @Parameter(description = "Book identifier") @PathVariable Long bookId) {
        return bookService.get(bookId);
    }
}