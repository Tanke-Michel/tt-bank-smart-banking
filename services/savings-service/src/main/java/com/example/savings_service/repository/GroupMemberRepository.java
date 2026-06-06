package com.example.savings_service.repository;

import com.example.savings_service.entity.GroupMember;
import com.example.savings_service.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroupIdOrderByPayoutOrder(Long groupId);

    List<GroupMember> findByGroupIdAndStatus(Long groupId, MemberStatus status);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    int countByGroupId(Long groupId);

    int countByGroupIdAndStatus(Long groupId, MemberStatus status);

    /** Member whose payout_order equals the group's current round */
    @Query("""
            SELECT m FROM GroupMember m
            WHERE m.group.id = :groupId
              AND m.payoutOrder = :round
              AND m.status = 'ACTIVE'
            """)
    Optional<GroupMember> findPayoutRecipientForRound(
            @Param("groupId") Long groupId,
            @Param("round")   int round);
}
