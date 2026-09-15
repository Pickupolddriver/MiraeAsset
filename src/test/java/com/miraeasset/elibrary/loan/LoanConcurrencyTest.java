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
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(attempts);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < attempts; i++) {
                String userId = "user-" + i;
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        loanService.borrow(userId, new BorrowBookRequest(book.getId()));
                        return true;
                    } catch (RuntimeException exception) {
                        return false;
                    }
                }));
            }
            ready.await();
            start.countDown();

            long successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    successes++;
                }
            }

            assertThat(successes).isEqualTo(3);
            assertThat(loanRepository.findByUserIdAndReturnedAtIsNullOrderByBorrowedAtDesc("user-0").size())
                    .isLessThanOrEqualTo(1);
            assertThat(bookRepository.findById(book.getId()).orElseThrow().getAvailableLicenses()).isZero();
            assertThat(loanRepository.count()).isEqualTo(3);
        } finally {
            executor.shutdownNow();
        }
    }
}
