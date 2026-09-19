package com.kmj.concert.api;

import com.kmj.concert.domain.Hold;
import com.kmj.concert.domain.HoldRepository;
import com.kmj.concert.domain.PaymentEvent;
import com.kmj.concert.domain.PaymentEventRepository;
import com.kmj.concert.domain.Seat;
import com.kmj.concert.domain.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.kmj.concert.domain.HoldStatus;

import java.util.List;

@Service
public class PaymentEventService {

    private final PaymentEventRepository paymentEventRepository;
    private final HoldRepository holdRepository;
    private final SeatRepository seatRepository;
    private final HoldExpirationService holdExpirationService;

    public PaymentEventService(
            PaymentEventRepository paymentEventRepository,
            HoldRepository holdRepository,
            SeatRepository seatRepository,
            HoldExpirationService holdExpirationService
    ) {
        this.paymentEventRepository = paymentEventRepository;
        this.holdRepository = holdRepository;
        this.seatRepository = seatRepository;
        this.holdExpirationService = holdExpirationService;
    }

    @Transactional
    public void receive(PaymentEventRequest request) {
        holdExpirationService.expireDueHolds();

        Hold hold = holdRepository.findByIdForUpdate(request.holdId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Hold not found"
                ));

        if (!hold.getPaymentId().equals(request.paymentId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Payment does not belong to this hold"
            );
        }

        if (paymentEventRepository.existsById(request.eventId())) {
            return;
        }

        paymentEventRepository.save(new PaymentEvent(
                request.eventId(),
                request.paymentId(),
                request.holdId(),
                request.status(),
                request.occurredAt()
        ));

        if (!hold.shouldApplyPaymentEvent(
                request.status(),
                request.occurredAt()
        )) {
            return;
        }

        hold.applyPaymentEvent(request.status(), request.occurredAt());

        if (request.status().equals("approved")) {
            if (!hold.isActive()) {
                return;
            }

            List<Seat> seats = seatRepository.findAllByHoldIdForUpdate(hold.getId());

            seats.forEach(seat -> seat.sell(hold.getId()));
            hold.markPaid();
            return;
        }

        if (request.status().equals("cancelled")
                && (hold.getStatus() == HoldStatus.ACTIVE
                || hold.getStatus() == HoldStatus.PAID)) {

            List<Seat> seats = seatRepository.findAllByHoldIdForUpdate(hold.getId());

            seats.forEach(seat -> seat.releaseForPaymentCancellation(hold.getId()));
            hold.cancelForPayment();
        }
    }}