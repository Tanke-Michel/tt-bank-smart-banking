package com.example.savings_service.repository;

import com.example.savings_service.entity.Payout;
import com.example.savings_service.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    Optional<Payout> findByGroupIdAndRoundNumber(Long groupId, int roundNumber);

    List<Payout> findByGroupIdOrderByRoundNumber(Long groupId);

    Page<Payout> findByRecipientMemberIdOrderByRoundNumberDesc(Long memberId, Pageable pageable);

    boolean existsByReferenceCode(String referenceCode);

    long countByGroupIdAndStatus(Long groupId, PayoutStatus status);
}
