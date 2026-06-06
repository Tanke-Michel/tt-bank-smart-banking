package com.example.savings_service.dto;

import com.example.savings_service.entity.GroupMember;
import com.example.savings_service.enums.MemberStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MemberResponse {

    private Long id;
    private Long groupId;
    private Long userId;
    private String userEmail;
    private String fullName;
    private String walletNumber;
    private int payoutOrder;
    private MemberStatus status;
    private boolean hasReceivedPayout;
    private LocalDateTime joinedAt;

    public static MemberResponse from(GroupMember m) {
        return MemberResponse.builder()
                .id(m.getId())
                .groupId(m.getGroup().getId())
                .userId(m.getUserId())
                .userEmail(m.getUserEmail())
                .fullName(m.getFullName())
                .walletNumber(m.getWalletNumber())
                .payoutOrder(m.getPayoutOrder())
                .status(m.getStatus())
                .hasReceivedPayout(m.isHasReceivedPayout())
                .joinedAt(m.getJoinedAt())
                .build();
    }
}
