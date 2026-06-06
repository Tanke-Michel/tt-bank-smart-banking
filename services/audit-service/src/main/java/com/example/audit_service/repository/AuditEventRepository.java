package com.example.audit_service.repository;

import com.example.audit_service.entity.AuditEvent;
import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findByEventDomain(EventDomain domain, Pageable pageable);

    Page<AuditEvent> findByEventType(EventType eventType, Pageable pageable);

    Page<AuditEvent> findByActorEmail(String actorEmail, Pageable pageable);

    Optional<AuditEvent> findByReferenceCode(String referenceCode);

    Page<AuditEvent> findByReceivedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditEvent> findByEventDomainAndReceivedAtBetween(
            EventDomain domain, LocalDateTime from, LocalDateTime to, Pageable pageable);

    /** Full-text search across summary and actor email */
    @Query("""
            SELECT a FROM AuditEvent a
            WHERE (:domain IS NULL OR a.eventDomain = :domain)
              AND (:eventType IS NULL OR a.eventType = :eventType)
              AND (:actorEmail IS NULL OR LOWER(a.actorEmail) LIKE LOWER(CONCAT('%', :actorEmail, '%')))
              AND (:from IS NULL OR a.receivedAt >= :from)
              AND (:to IS NULL OR a.receivedAt <= :to)
            ORDER BY a.receivedAt DESC
            """)
    Page<AuditEvent> searchAuditEvents(
            @Param("domain")     EventDomain domain,
            @Param("eventType")  EventType eventType,
            @Param("actorEmail") String actorEmail,
            @Param("from")       LocalDateTime from,
            @Param("to")         LocalDateTime to,
            Pageable pageable);

    /** Count events per domain for the admin dashboard statistics */
    @Query("SELECT a.eventDomain, COUNT(a) FROM AuditEvent a GROUP BY a.eventDomain")
    List<Object[]> countByDomain();

    /** Count events per type for today */
    @Query("""
            SELECT a.eventType, COUNT(a)
            FROM AuditEvent a
            WHERE a.receivedAt >= :from
            GROUP BY a.eventType
            ORDER BY COUNT(a) DESC
            """)
    List<Object[]> countByTypeFrom(@Param("from") LocalDateTime from);

    long countByEventDomain(EventDomain domain);

    long countByReceivedAtBetween(LocalDateTime from, LocalDateTime to);
}
