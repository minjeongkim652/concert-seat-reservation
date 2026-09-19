package com.kmj.concert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_events")
public class PaymentEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Column(name = "hold_id", nullable = false)
    private UUID holdId;

    @Column(nullable = false)
    private String status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PaymentEvent() {
    }

    public PaymentEvent(
            String eventId,
            String paymentId,
            UUID holdId,
            String status,
            Instant occurredAt
    ) {
        this.eventId = eventId;
        this.paymentId = paymentId;
        this.holdId = holdId;
        this.status = status;
        this.occurredAt = occurredAt;
    }
}