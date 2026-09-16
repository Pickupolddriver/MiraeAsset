package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.book.Book;
import com.miraeasset.elibrary.book.BookRepository;
import com.miraeasset.elibrary.common.BusinessConflictException;
import com.miraeasset.elibrary.common.ForbiddenOperationException;
import com.miraeasset.elibrary.common.InvalidRequestException;
import com.miraeasset.elibrary.common.ResourceNotFoundException;
import com.miraeasset.elibrary.common.dto.PageResponse;
import com.miraeasset.elibrary.loan.dto.BorrowBookRequest;
import com.miraeasset.elibrary.loan.dto.LoanResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public LoanResponse borrow(String userId, BorrowBookRequest request) {
        requireUser(userId);
        Book book = bookRepository.findByIdForUpdate(request.bookId())
                .orElseThrow(() -> new ResourceNotFoundException("BOOK_NOT_FOUND", "Book does not exist"));

        if (loanRepository.existsByBookIdAndUserIdAndReturnedAtIsNull(book.getId(), userId)) {
            throw new BusinessConflictException("ACTIVE_LOAN_ALREADY_EXISTS",
                    "The user already has an active loan for this book");
        }
        if (book.getAvailableLicenses() == 0) {
            throw new BusinessConflictException("BOOK_UNAVAILABLE", "No digital license is available");
        }

        book.borrowLicense();
        Loan loan = loanRepository.save(Loan.create(book, userId, Instant.now(clock)));
        return LoanResponse.from(loan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> currentLoans(String userId) {
        requireUser(userId);
        return loanRepository.findByUserIdAndReturnedAtIsNullOrderByBorrowedAtDesc(userId)
                .stream()
                .map(LoanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> allCurrentLoans(int page, int size) {
        if (page < 0) {
            throw new InvalidRequestException("INVALID_PAGE", "page must not be negative");
        }
        if (size < 1 || size > 50) {
            throw new InvalidRequestException("INVALID_PAGE_SIZE", "size must be between 1 and 50");
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("borrowedAt").descending());
        Page<Loan> loans = loanRepository.findAllByReturnedAtIsNull(pageRequest);
        return PageResponse.from(loans.map(LoanResponse::from));
    }

    @Transactional
    public LoanResponse returnLoan(String userId, UUID loanId) {
        requireUser(userId);
        Loan initialLoan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("LOAN_NOT_FOUND", "Loan does not exist"));
        if (!initialLoan.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("LOAN_NOT_OWNED_BY_USER", "The loan belongs to another user");
        }

        Long bookId = initialLoan.getBook().getId();
        bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("BOOK_NOT_FOUND", "Book does not exist"));

        // The loan may have been returned while this transaction waited for the book lock.
        // Refresh it so a concurrent repeated return remains idempotent.
        entityManager.refresh(initialLoan);
        if (!initialLoan.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("LOAN_NOT_OWNED_BY_USER", "The loan belongs to another user");
        }
        if (!initialLoan.isActive()) {
            return LoanResponse.from(initialLoan);
        }

        initialLoan.markReturned(Instant.now(clock));
        initialLoan.getBook().returnLicense();
        return LoanResponse.from(initialLoan);
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new InvalidRequestException("INVALID_USER", "X-User-Id must not be blank");
        }
        if (userId.length() > 100) {
            throw new InvalidRequestException("INVALID_USER", "X-User-Id must not exceed 100 characters");
        }
    }
}
