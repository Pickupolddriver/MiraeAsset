package com.miraeasset.elibrary.loan;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    boolean existsByBookIdAndUserIdAndReturnedAtIsNull(Long bookId, String userId);

    @EntityGraph(attributePaths = "book")
    List<Loan> findByUserIdAndReturnedAtIsNullOrderByBorrowedAtDesc(String userId);

    @EntityGraph(attributePaths = "book")
    Page<Loan> findAllByReturnedAtIsNull(Pageable pageable);
}