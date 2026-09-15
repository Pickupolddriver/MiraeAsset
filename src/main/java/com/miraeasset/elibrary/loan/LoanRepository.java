package com.miraeasset.elibrary.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    boolean existsByBookIdAndUserIdAndReturnedAtIsNull(Long bookId, String userId);

    List<Loan> findByUserIdAndReturnedAtIsNullOrderByBorrowedAtDesc(String userId);
}
