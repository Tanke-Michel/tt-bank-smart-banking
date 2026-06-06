package com.example.transaction_service.repository;

import com.example.transaction_service.entity.Transaction;
import com.example.transaction_service.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);

    /** All transactions where the user is either sender or receiver — for history page */
    @Query("""
            SELECT t FROM Transaction t
            WHERE t.senderUserId = :userId OR t.receiverUserId = :userId
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    /** Transactions where user is the sender — for outgoing transfers view */
    Page<Transaction> findBySenderUserIdOrderByCreatedAtDesc(Long senderUserId, Pageable pageable);

    /** Transactions where user is the receiver — for incoming transfers view */
    Page<Transaction> findByReceiverUserIdOrderByCreatedAtDesc(Long receiverUserId, Pageable pageable);

    /**
     * Sum of all completed outgoing transfers today.
     * Used for daily transfer limit enforcement.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.senderUserId = :userId
              AND t.status = :status
              AND t.createdAt >= :from
              AND t.createdAt < :to
            """)
    BigDecimal sumCompletedAmountByUserAndDateRange(
            @Param("userId") Long userId,
            @Param("status") TransactionStatus status,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);
}
