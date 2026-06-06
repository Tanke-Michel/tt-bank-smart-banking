package com.example.merchant_service.repository;

import com.example.merchant_service.entity.Merchant;
import com.example.merchant_service.enums.MerchantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByMerchantCode(String merchantCode);

    Optional<Merchant> findByOwnerUserId(Long ownerUserId);

    Optional<Merchant> findByBusinessEmail(String businessEmail);

    boolean existsByOwnerUserId(Long ownerUserId);

    boolean existsByBusinessEmail(String businessEmail);

    Page<Merchant> findByStatus(MerchantStatus status, Pageable pageable);

    Page<Merchant> findByOwnerUserId(Long ownerUserId, Pageable pageable);
}
