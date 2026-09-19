package com.kmj.concert.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @Column(nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status;

    @Column(name = "hold_id")
    private UUID holdId;

    protected Seat() {
    }

    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    public void hold(UUID holdId) {
        if (!isAvailable()) {
            throw new IllegalStateException("판매 가능한 좌석만 선점할 수 있습니다.");
        }

        this.status = SeatStatus.HELD;
        this.holdId = holdId;
    }

    public void release() {
        if (status != SeatStatus.HELD) {
            throw new IllegalStateException("선점된 좌석만 해제할 수 있습니다.");
        }

        this.status = SeatStatus.AVAILABLE;
        this.holdId = null;
    }

    public void releaseForPaymentCancellation(UUID holdId) {
        if (!holdId.equals(this.holdId)) {
            throw new IllegalStateException("This seat does not belong to this hold");
        }

        if (status != SeatStatus.HELD && status != SeatStatus.SOLD) {
            throw new IllegalStateException("This seat cannot be released");
        }

        this.status = SeatStatus.AVAILABLE;
        this.holdId = null;
    }
    public void sell(UUID holdId) {
        if (status != SeatStatus.HELD || !this.holdId.equals(holdId)) {
            throw new IllegalStateException("This seat is not held by this hold");
        }

        this.status = SeatStatus.SOLD;
    }

    public Long getId() {
        return id;
    }

    public Performance getPerformance() {
        return performance;
    }

    public String getLabel() {
        return label;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public UUID getHoldId() {
        return holdId;
    }


}