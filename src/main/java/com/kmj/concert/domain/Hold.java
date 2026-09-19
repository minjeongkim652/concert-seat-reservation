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

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "payment_occurred_at")
    private Instant paymentOccurredAt;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    protected Hold() {
    }

    private Hold(
            UUID id,
            Performance performance,
            String userId,
            String requestId,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.id = id;
        this.paymentId = "pay-" + id;
        this.paymentStatus = "PENDING";
        this.performance = performance;
        this.userId = userId;
        this.requestId = requestId;
        this.status = HoldStatus.ACTIVE;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    public String getPaymentId() {
        return paymentId;
    }
    public boolean shouldApplyPaymentEvent(String eventStatus, Instant occurredAt) {
        if (paymentOccurredAt == null) {
            return true;
        }

        int comparison = occurredAt.compareTo(paymentOccurredAt);

        if (comparison > 0) {
            return true;
        }

        if (comparison < 0) {
            return false;
        }

        return eventStatus.equals("cancelled")
                && !paymentStatus.equals("CANCELLED");
    }

    public void applyPaymentEvent(String eventStatus, Instant occurredAt) {
        this.paymentStatus = eventStatus.toUpperCase();
        this.paymentOccurredAt = occurredAt;
    }
    public static Hold createActive(
            Performance performance,
            String userId,
            String requestId,
            Instant now,
            Duration holdDuration
    ) {
        return new Hold(
                UUID.randomUUID(),
                performance,
                userId,
                requestId,
                now,
                now.plus(holdDuration)
        );
    }
    public String getRequestId() {
        return requestId;
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
    public void cancelForPayment() {
        if (status != HoldStatus.ACTIVE && status != HoldStatus.PAID) {
            throw new IllegalStateException("This hold cannot be cancelled by payment");
        }

        this.status = HoldStatus.CANCELLED;
    }
    public void markPaid() {
        if (status != HoldStatus.ACTIVE) {
            throw new IllegalStateException("Only an active hold can be paid");
        }

        this.status = HoldStatus.PAID;
    }
}