package com.kmj.concert.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findAllByPerformance_IdOrderById(Long performanceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE) //좌석 확인하는 동안 다른 요청 기달 (db rock)
    @Query("""
            select seat
            from Seat seat
            where seat.performance.id = :performanceId
              and seat.label in :seatLabels
            order by seat.id
            """)
    List<Seat> findAllForUpdate(
            @Param("performanceId") Long performanceId,
            @Param("seatLabels") List<String> seatLabels
    );
}