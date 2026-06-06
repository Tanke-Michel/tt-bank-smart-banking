package com.example.wallet_service.repository;

import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.enums.WalletStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    Optional<Wallet> findByWalletNumber(String walletNumber);

    Optional<Wallet> findByEmail(String email);

    Optional<Wallet> findByPhoneNumber(String phoneNumber);

    boolean existsByUserId(Long userId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    /**
     * Pessimistic write lock — used during deposit/withdrawal to prevent
     * concurrent balance corruption at the database level.
     * Complements optimistic locking (@Version) for defence in depth.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.id = :id")
    Optional<Wallet> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
    Optional<Wallet> findByUserIdWithLock(@Param("userId") Long userId);
}
