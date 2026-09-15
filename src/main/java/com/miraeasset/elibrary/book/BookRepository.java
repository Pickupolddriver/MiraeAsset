package com.miraeasset.elibrary.book;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
            select b from Book b
            where (:query is null or :query = ''
                or lower(b.title) like lower(concat('%', :query, '%'))
                or lower(b.author) like lower(concat('%', :query, '%')))
              and (:category is null or :category = '' or lower(b.category) = lower(:category))
            """)
    Page<Book> search(@Param("query") String query,
                      @Param("category") String category,
                      Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") Long id);
}