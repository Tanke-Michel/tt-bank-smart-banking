package com.example.merchant_service.repository;

import com.example.merchant_service.entity.MerchantPayment;
import com.example.merchant_service.enums.PaymentStatus;
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
public interface MerchantPaymentRepository extends JpaRepository<MerchantPayment, Long> {

    Optional<MerchantPayment> findByReferenceCode(String referenceCode);

    boolean existsByReferenceCode(String referenceCode);

    Page<MerchantPayment> findByMerchantIdOrderByCreatedAtDesc(Long merchantId, Pageable pageable);

    Page<MerchantPayment> findByCustomerUserIdOrderByCreatedAtDesc(Long customerUserId, Pageable pageable);

    /** Total revenue (completed payments) for a merchant in a date range */
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0)
            FROM MerchantPayment p
            WHERE p.merchant.id = :merchantId
              AND p.status = :status
              AND p.createdAt >= :from
              AND p.createdAt < :to
            """)
    BigDecimal sumCompletedByMerchantAndDateRange(
            @Param("merchantId") Long merchantId,
            @Param("status")     PaymentStatus status,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);

    /** Count of payments for a merchant in a date range */
    @Query("""
            SELECT COUNT(p)
            FROM MerchantPayment p
            WHERE p.merchant.id = :merchantId
              AND p.status = :status
              AND p.createdAt >= :from
              AND p.createdAt < :to
            """)
    long countByMerchantAndDateRange(
            @Param("merchantId") Long merchantId,
            @Param("status")     PaymentStatus status,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to);
}
