package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.book.Book;
import com.miraeasset.elibrary.book.BookRepository;
import com.miraeasset.elibrary.common.BusinessConflictException;
import com.miraeasset.elibrary.common.ForbiddenOperationException;
import com.miraeasset.elibrary.common.InvalidRequestException;
import com.miraeasset.elibrary.common.ResourceNotFoundException;
import com.miraeasset.elibrary.loan.dto.BorrowBookRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class LoanServiceTest {

    @Autowired
    private LoanService loanService;

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
    void borrowAndReturnMaintainsAvailability() {
        Book book = bookRepository.save(Book.create("isbn-1", "Book 1", "Author 1", "Description", "Tech", 1));

        var loan = loanService.borrow("user-1", new BorrowBookRequest(book.getId()));
        assertThat(loan.status()).isEqualTo("ACTIVE");
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isZero();

        var returned = loanService.returnLoan("user-1", loan.loanId());
        assertThat(returned.status()).isEqualTo("RETURNED");
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isEqualTo(1);
    }

    @Test
    void duplicateBorrowIsRejected() {
        Book book = bookRepository.save(Book.create("isbn-2", "Book 2", "Author 2", "Description", "Tech", 2));
        loanService.borrow("user-1", new BorrowBookRequest(book.getId()));

        assertThatThrownBy(() -> loanService.borrow("user-1", new BorrowBookRequest(book.getId())))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("already has an active loan");
    }

    @Test
    void unavailableBookIsRejected() {
        Book book = bookRepository.save(Book.create("isbn-3", "Book 3", "Author 3", "Description", "Tech", 1));
        loanService.borrow("user-1", new BorrowBookRequest(book.getId()));

        assertThatThrownBy(() -> loanService.borrow("user-2", new BorrowBookRequest(book.getId())))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("No digital license is available");
    }

    @Test
    void missingBookIsNotFound() {
        assertThatThrownBy(() -> loanService.borrow("user-1", new BorrowBookRequest(999L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Book does not exist");
    }

    @Test
    void returnMustBelongToRequestingUser() {
        Book book = bookRepository.save(Book.create("isbn-4", "Book 4", "Author 4", "Description", "Tech", 1));
        var loan = loanService.borrow("owner", new BorrowBookRequest(book.getId()));

        assertThatThrownBy(() -> loanService.returnLoan("other-user", loan.loanId()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("belongs to another user");
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isZero();
    }

    @Test
    void repeatedReturnIsIdempotent() {
        Book book = bookRepository.save(Book.create("isbn-5", "Book 5", "Author 5", "Description", "Tech", 1));
        var loan = loanService.borrow("user-1", new BorrowBookRequest(book.getId()));

        loanService.returnLoan("user-1", loan.loanId());
        var repeated = loanService.returnLoan("user-1", loan.loanId());

        assertThat(repeated.status()).isEqualTo("RETURNED");
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isEqualTo(1);
    }

    @Test
    void blankUserIsRejectedAsInvalidRequest() {
        Book book = bookRepository.save(Book.create("isbn-6", "Book 6", "Author 6", "Description", "Tech", 1));

        assertThatThrownBy(() -> loanService.borrow(" ", new BorrowBookRequest(book.getId())))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("must not be blank");
    }
}