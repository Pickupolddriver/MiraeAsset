package com.miraeasset.elibrary.loan;

import com.miraeasset.elibrary.book.Book;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Loan {

    private static final int LOAN_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    private String userId;
    private Instant borrowedAt;
    private Instant dueAt;
    private Instant returnedAt;

    private Loan(Book book, String userId, Instant borrowedAt) {
        this.book = book;
        this.userId = userId;
        this.borrowedAt = borrowedAt;
        this.dueAt = borrowedAt.plus(LOAN_DAYS, ChronoUnit.DAYS);
    }

    public static Loan create(Book book, String userId, Instant borrowedAt) {
        return new Loan(book, userId, borrowedAt);
    }

    public boolean isActive() {
        return returnedAt == null;
    }

    public void markReturned(Instant returnedAt) {
        if (!isActive()) {
            return;
        }
        this.returnedAt = returnedAt;
    }
}
