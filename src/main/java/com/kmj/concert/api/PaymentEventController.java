package com.kmj.concert.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentEventController {

    private final PaymentEventService paymentEventService;

    public PaymentEventController(PaymentEventService paymentEventService) {
        this.paymentEventService = paymentEventService;
    }

    @PostMapping("/payment-events")
    public ResponseEntity<Void> receive(
            @Valid @RequestBody PaymentEventRequest request
    ) {
        paymentEventService.receive(request);
        return ResponseEntity.noContent().build();
    }
}