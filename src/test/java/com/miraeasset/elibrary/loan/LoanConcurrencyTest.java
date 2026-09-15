package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.book.Book;
import com.miraeasset.elibrary.book.BookRepository;
import com.miraeasset.elibrary.loan.dto.BorrowBookRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoanConcurrencyTest {

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
    void onlyAvailableLicensesCanBeBorrowedConcurrently() throws Exception {
        Book book = bookRepository.save(Book.create("isbn-concurrent", "Concurrent Book", "Author", "Description", "Tech", 3));
        int attempts = 10;
        List<Boolean> results = runConcurrently(attempts, () -> {
            String userId = "user-" + Thread.currentThread().getId();
            loanService.borrow(userId, new BorrowBookRequest(book.getId()));
        });

        assertThat(results.stream().filter(Boolean.TRUE::equals).count()).isEqualTo(3);
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isZero();
        assertThat(loanRepository.count()).isEqualTo(3);
    }

    @Test
    void sameUserCanOnlyBorrowTheSameBookOnceConcurrently() throws Exception {
        Book book = bookRepository.save(Book.create("isbn-same-user", "Same User Book", "Author", "Description", "Tech", 10));
        int attempts = 10;
        List<Boolean> results = runConcurrently(attempts, () ->
                loanService.borrow("same-user", new BorrowBookRequest(book.getId())));

        assertThat(results.stream().filter(Boolean.TRUE::equals).count()).isEqualTo(1);
        assertThat(loanRepository.count()).isEqualTo(1);
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isEqualTo(9);
    }

    @Test
    void repeatedReturnsAreIdempotentWhenConcurrent() throws Exception {
        Book book = bookRepository.save(Book.create("isbn-return-race", "Return Race Book", "Author", "Description", "Tech", 1));
        var loan = loanService.borrow("return-user", new BorrowBookRequest(book.getId()));
        int attempts = 10;

        List<Boolean> results = runConcurrently(attempts, () ->
                loanService.returnLoan("return-user", loan.loanId()));

        assertThat(results).containsOnly(true);
        assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isEqualTo(1);
        assertThat(loanRepository.findByUserIdAndReturnedAtIsNullOrderByBorrowedAtDesc("return-user")).isEmpty();
    }

    private List<Boolean> runConcurrently(int attempts, Runnable action) throws Exception {
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<Boolean>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        action.run();
                        return true;
                    } catch (RuntimeException exception) {
                        return false;
                    }
                }));
            }
            ready.await();
            start.countDown();

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}