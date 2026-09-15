package com.miraeasset.elibrary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miraeasset.elibrary.book.Book;
import com.miraeasset.elibrary.book.BookRepository;
import com.miraeasset.elibrary.loan.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
    }

    @Test
    void canBrowseAndViewBookDetails() throws Exception {
        Book book = bookRepository.save(Book.create("isbn-api", "API Book", "API Author", "A description", "Tech", 2));

        mockMvc.perform(get("/api/books").param("q", "api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("API Book"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/books/{bookId}", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("A description"))
                .andExpect(jsonPath("$.availableLicenses").value(2));
    }

    @Test
    void canBorrowViewCurrentLoansAndReturn() throws Exception {
        Book book = bookRepository.save(Book.create("isbn-flow", "Flow Book", "Author", "Description", "Tech", 1));

        String response = mockMvc.perform(post("/api/loans")
                        .header("X-User-Id", "user-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + book.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        JsonNode loan = objectMapper.readTree(response);

        mockMvc.perform(get("/api/loans/current").header("X-User-Id", "user-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put("/api/loans/{loanId}/return", loan.get("loanId").asText())
                        .header("X-User-Id", "user-api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void rejectsInvalidPaginationAndMalformedRequests() throws Exception {
        mockMvc.perform(get("/api/books").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE"));

        mockMvc.perform(get("/api/books").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));

        mockMvc.perform(get("/api/books/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/loans")
                        .header("X-User-Id", "user-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/loans")
                        .header("X-User-Id", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_USER"));
    }

    @Test
    void returnsStableErrorsForMissingResourcesAndHeaders() throws Exception {
        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"));

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/loans")
                        .header("X-User-Id", "user-api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void cannotReturnAnotherUsersLoan() throws Exception {
        Book book = bookRepository.save(Book.create("isbn-owner", "Owner Book", "Author", "Description", "Tech", 1));
        String response = mockMvc.perform(post("/api/loans")
                        .header("X-User-Id", "owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\":" + book.getId() + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String loanId = objectMapper.readTree(response).get("loanId").asText();

        mockMvc.perform(put("/api/loans/{loanId}/return", loanId)
                        .header("X-User-Id", "other-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LOAN_NOT_OWNED_BY_USER"));
    }
}