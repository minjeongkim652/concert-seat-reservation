package com.kmj.concert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "holds")
public class Hold {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HoldStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Hold() {
    }

    private Hold(
            UUID id,
            Performance performance,
            String userId,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.performance = performance;
        this.userId = userId;
        this.status = HoldStatus.ACTIVE;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public static Hold createActive(
            Performance performance,
            String userId,
            Instant now,
            Duration holdDuration
    ) {
        return new Hold(
                UUID.randomUUID(),
                performance,
                userId,
                now,
                now.plus(holdDuration)
        );
    }

    public UUID getId() {
        return id;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isActive() {
        return status == HoldStatus.ACTIVE;
    }

    public void cancel() {
        if (!isActive()) {
            throw new IllegalStateException("유효한 선점만 취소할 수 있습니다.");
        }

        this.status = HoldStatus.CANCELLED;
    }
}