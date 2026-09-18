package com.miraeasset.elibrary.book;

import com.miraeasset.elibrary.book.dto.BookDetailResponse;
import com.miraeasset.elibrary.book.dto.BookSummaryResponse;
import com.miraeasset.elibrary.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
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
    @ApiResponses(@ApiResponse(responseCode = "400", description = "Invalid pagination or request",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))))
    public PageResponse<BookSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return bookService.list(q, category, page, size);
    }

    @GetMapping("/{bookId}")
    @Operation(summary = "Get book details")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Invalid book identifier",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Book not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public BookDetailResponse get(
            @Parameter(description = "Book identifier") @PathVariable Long bookId) {
        return bookService.get(bookId);
    }
}
