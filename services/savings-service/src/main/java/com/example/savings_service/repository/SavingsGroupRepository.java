package com.example.savings_service.repository;

import com.example.savings_service.entity.SavingsGroup;
import com.example.savings_service.enums.GroupStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGroupRepository extends JpaRepository<SavingsGroup, Long> {

    Page<SavingsGroup> findByStatus(GroupStatus status, Pageable pageable);

    Page<SavingsGroup> findByCreatorUserId(Long creatorUserId, Pageable pageable);

    /** Groups the given user is a member of (any status) */
    @Query("""
            SELECT g FROM SavingsGroup g
            JOIN g.members m
            WHERE m.userId = :userId
            ORDER BY g.createdAt DESC
            """)
    Page<SavingsGroup> findGroupsByMemberUserId(@Param("userId") Long userId, Pageable pageable);

    /** Active groups the user is a member of */
    @Query("""
            SELECT g FROM SavingsGroup g
            JOIN g.members m
            WHERE m.userId = :userId
              AND g.status = :status
            """)
    List<SavingsGroup> findGroupsByMemberUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") GroupStatus status);

    boolean existsByNameAndCreatorUserId(String name, Long creatorUserId);
}
