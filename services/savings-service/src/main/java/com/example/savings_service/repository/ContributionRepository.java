package com.example.savings_service.repository;

import com.example.savings_service.entity.Contribution;
import com.example.savings_service.enums.ContributionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContributionRepository extends JpaRepository<Contribution, Long> {

    Optional<Contribution> findByMemberIdAndRoundNumber(Long memberId, int roundNumber);

    List<Contribution> findByGroupIdAndRoundNumber(Long groupId, int roundNumber);

    Page<Contribution> findByMemberIdOrderByRoundNumberDesc(Long memberId, Pageable pageable);

    Page<Contribution> findByGroupIdOrderByRoundNumberDescCreatedAtDesc(Long groupId, Pageable pageable);

    long countByGroupIdAndRoundNumberAndStatus(Long groupId, int roundNumber, ContributionStatus status);

    boolean existsByReferenceCode(String referenceCode);

    /** Sum of PAID contributions for a group in a given round */
    @Query("""
            SELECT COALESCE(SUM(c.amount), 0)
            FROM Contribution c
            WHERE c.group.id = :groupId
              AND c.roundNumber = :round
              AND c.status = 'PAID'
            """)
    BigDecimal sumPaidAmountForRound(
            @Param("groupId") Long groupId,
            @Param("round")   int round);

    /** Total contributions paid by a member across all rounds */
    @Query("""
            SELECT COALESCE(SUM(c.amount), 0)
            FROM Contribution c
            WHERE c.member.id = :memberId
              AND c.status = 'PAID'
            """)
    BigDecimal sumTotalPaidByMember(@Param("memberId") Long memberId);
}
