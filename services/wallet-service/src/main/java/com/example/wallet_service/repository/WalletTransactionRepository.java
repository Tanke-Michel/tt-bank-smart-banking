package com.example.wallet_service.repository;

import com.example.wallet_service.entity.WalletTransaction;
import com.example.wallet_service.enums.TransactionType;
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
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    Page<WalletTransaction> findByWalletIdAndTypeOrderByCreatedAtDesc(
            Long walletId, TransactionType type, Pageable pageable);

    Optional<WalletTransaction> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);

    /** Total amount deposited/withdrawn between two dates — used for daily limit checks. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM WalletTransaction t
            WHERE t.wallet.id = :walletId
              AND t.type = :type
              AND t.createdAt >= :from
              AND t.createdAt < :to
            """)
    BigDecimal sumAmountByWalletIdAndTypeAndCreatedAtBetween(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
