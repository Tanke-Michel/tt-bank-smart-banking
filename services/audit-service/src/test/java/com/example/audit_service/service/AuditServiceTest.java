package com.example.audit_service.service;

import com.example.audit_service.dto.AuditEventResponse;
import com.example.audit_service.dto.AuditStatsResponse;
import com.example.audit_service.dto.PagedResponse;
import com.example.audit_service.entity.AuditEvent;
import com.example.audit_service.enums.EventDomain;
import com.example.audit_service.enums.EventType;
import com.example.audit_service.exception.AuditEventNotFoundException;
import com.example.audit_service.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("AuditService Unit Tests")
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditEventRepository auditEventRepository;
    @InjectMocks private AuditService auditService;

    @BeforeEach
    void setUp() {
        // Inject real ObjectMapper so serialization works
        try {
            var field = AuditService.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(auditService, new ObjectMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ================================================================
    // persistEvent — wallet domain
    // ================================================================

    @Test
    @DisplayName("persistEvent — saves WALLET_CREATED with correct fields")
    void persistEvent_walletCreated_savesCorrectly() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WALLET_CREATED");
        event.put("email", "user@example.com");
        event.put("walletNumber", "WLT-20240101-ABCD1234");
        event.put("currency", "XAF");
        event.put("timestamp", "2024-01-01T10:00:00");

        when(auditEventRepository.save(any())).thenAnswer(inv -> {
            AuditEvent e = inv.getArgument(0);
            e = AuditEvent.builder()
                    .id(1L).eventDomain(e.getEventDomain()).eventType(e.getEventType())
                    .actorEmail(e.getActorEmail()).summary(e.getSummary())
                    .rawPayload(e.getRawPayload()).build();
            return e;
        });

        auditService.persistEvent(EventDomain.WALLET, EventType.WALLET_CREATED, event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getEventDomain()).isEqualTo(EventDomain.WALLET);
        assertThat(saved.getEventType()).isEqualTo(EventType.WALLET_CREATED);
        assertThat(saved.getActorEmail()).isEqualTo("user@example.com");
        assertThat(saved.getRawPayload()).contains("walletNumber");
        assertThat(saved.getSummary()).contains("user@example.com");
    }

    @Test
    @DisplayName("persistEvent — saves TRANSACTION_COMPLETED with referenceCode")
    void persistEvent_transactionCompleted_savesReferenceCode() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TRANSACTION_COMPLETED");
        event.put("referenceCode", "TXN-20240101-ABCD1234");
        event.put("senderEmail", "sender@example.com");
        event.put("receiverEmail", "receiver@example.com");
        event.put("amount", "500.00");
        event.put("currency", "XAF");

        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.persistEvent(EventDomain.TRANSACTION, EventType.TRANSACTION_COMPLETED, event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getReferenceCode()).isEqualTo("TXN-20240101-ABCD1234");
        assertThat(saved.getActorEmail()).isEqualTo("sender@example.com");
        assertThat(saved.getSummary()).contains("completed").contains("500.00");
    }

    @Test
    @DisplayName("persistEvent — saves MERCHANT_PAYMENT_COMPLETED with correct actor")
    void persistEvent_merchantPaymentCompleted_savesCustomerAsActor() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "MERCHANT_PAYMENT_COMPLETED");
        event.put("customerEmail", "customer@example.com");
        event.put("businessName", "Jean's Shop");
        event.put("amount", "2500.00");
        event.put("currency", "XAF");
        event.put("referenceCode", "PAY-20240101-ABCD1234");

        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.persistEvent(EventDomain.MERCHANT, EventType.MERCHANT_PAYMENT_COMPLETED, event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getActorEmail()).isEqualTo("customer@example.com");
        assertThat(saved.getReferenceCode()).isEqualTo("PAY-20240101-ABCD1234");
        assertThat(saved.getSummary()).contains("Jean's Shop");
    }

    @Test
    @DisplayName("persistEvent — saves SAVINGS_PAYOUT_PROCESSED with correct actor")
    void persistEvent_savingsPayoutProcessed_savesRecipientAsActor() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "SAVINGS_PAYOUT_PROCESSED");
        event.put("recipientEmail", "recipient@example.com");
        event.put("groupName", "Njangi Circle");
        event.put("roundNumber", 1);
        event.put("amount", "15000.00");
        event.put("currency", "XAF");
        event.put("referenceCode", "POUT-20240101-ABCD1234");

        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        auditService.persistEvent(EventDomain.SAVINGS, EventType.SAVINGS_PAYOUT_PROCESSED, event);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent saved = captor.getValue();
        assertThat(saved.getActorEmail()).isEqualTo("recipient@example.com");
        assertThat(saved.getSummary()).contains("Njangi Circle");
    }

    @Test
    @DisplayName("persistEvent — null fields handled gracefully, record still saved")
    void persistEvent_nullFields_stillSaves() {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WALLET_FUNDED");
        // no email, no referenceCode, no amount

        when(auditEventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() ->
                auditService.persistEvent(EventDomain.WALLET, EventType.WALLET_FUNDED, event))
                .doesNotThrowAnyException();

        verify(auditEventRepository).save(any());
    }

    // ================================================================
    // getById
    // ================================================================

    @Test
    @DisplayName("getById — returns event for known ID")
    void getById_found_returnsResponse() {
        AuditEvent event = buildSampleEvent(1L, EventDomain.WALLET, EventType.WALLET_CREATED);
        when(auditEventRepository.findById(1L)).thenReturn(Optional.of(event));

        AuditEventResponse response = auditService.getById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEventDomain()).isEqualTo(EventDomain.WALLET);
        assertThat(response.getEventType()).isEqualTo(EventType.WALLET_CREATED);
    }

    @Test
    @DisplayName("getById — throws AuditEventNotFoundException for unknown ID")
    void getById_notFound_throws() {
        when(auditEventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getById(999L))
                .isInstanceOf(AuditEventNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ================================================================
    // getByReferenceCode
    // ================================================================

    @Test
    @DisplayName("getByReferenceCode — returns event for known reference")
    void getByReferenceCode_found_returnsResponse() {
        AuditEvent event = buildSampleEvent(2L, EventDomain.TRANSACTION, EventType.TRANSACTION_COMPLETED);
        event.setReferenceCode("TXN-20240101-ABCD1234");
        when(auditEventRepository.findByReferenceCode("TXN-20240101-ABCD1234"))
                .thenReturn(Optional.of(event));

        AuditEventResponse response = auditService.getByReferenceCode("TXN-20240101-ABCD1234");

        assertThat(response.getReferenceCode()).isEqualTo("TXN-20240101-ABCD1234");
    }

    @Test
    @DisplayName("getByReferenceCode — throws AuditEventNotFoundException for unknown reference")
    void getByReferenceCode_notFound_throws() {
        when(auditEventRepository.findByReferenceCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getByReferenceCode("UNKNOWN"))
                .isInstanceOf(AuditEventNotFoundException.class);
    }

    // ================================================================
    // search
    // ================================================================

    @Test
    @DisplayName("search — returns paged results with filters")
    void search_withFilters_returnsPaged() {
        AuditEvent event = buildSampleEvent(1L, EventDomain.TRANSACTION, EventType.TRANSACTION_COMPLETED);
        when(auditEventRepository.searchAuditEvents(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(event)));

        PagedResponse<AuditEventResponse> response = auditService.search(
                EventDomain.TRANSACTION, null, null, null, null,
                PageRequest.of(0, 50));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    // ================================================================
    // getStats
    // ================================================================

    @Test
    @DisplayName("getStats — returns correct total count")
    void getStats_returnsCorrectTotals() {
        when(auditEventRepository.count()).thenReturn(250L);
        when(auditEventRepository.countByReceivedAtBetween(any(), any())).thenReturn(15L);
        when(auditEventRepository.countByDomain()).thenReturn(List.of(
                new Object[]{EventDomain.WALLET, 100L},
                new Object[]{EventDomain.TRANSACTION, 150L}
        ));
        when(auditEventRepository.countByTypeFrom(any())).thenReturn(List.of(
                new Object[]{EventType.TRANSACTION_COMPLETED, 10L},
                new Object[]{EventType.WALLET_FUNDED, 5L}
        ));

        AuditStatsResponse stats = auditService.getStats();

        assertThat(stats.getTotalEvents()).isEqualTo(250L);
        assertThat(stats.getTodayEvents()).isEqualTo(15L);
        assertThat(stats.getByDomain()).containsKey("WALLET");
        assertThat(stats.getByDomain()).containsKey("TRANSACTION");
        assertThat(stats.getTodayByType()).containsKey("TRANSACTION_COMPLETED");
    }

    // ================================================================
    // Helper
    // ================================================================

    private AuditEvent buildSampleEvent(Long id, EventDomain domain, EventType type) {
        return AuditEvent.builder()
                .id(id)
                .eventDomain(domain)
                .eventType(type)
                .actorEmail("user@example.com")
                .summary("Test event")
                .rawPayload("{\"test\":true}")
                .receivedAt(LocalDateTime.now())
                .build();
    }
}
