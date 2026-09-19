package com.kmj.concert.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface HoldRepository extends JpaRepository<Hold, UUID> {
    Optional<Hold> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select hold
            from Hold hold
            where hold.id = :holdId
            """)
    Optional<Hold> findByIdForUpdate(@Param("holdId") UUID holdId);
}