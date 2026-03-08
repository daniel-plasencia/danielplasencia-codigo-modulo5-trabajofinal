package com.tecsup.app.micro.payment.application.usecase;

import com.tecsup.app.micro.payment.domain.model.Payment;
import com.tecsup.app.micro.payment.domain.repository.PaymentRepository;
import com.tecsup.app.micro.payment.infrastructure.kafka.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    
    public Payment execute(Long orderId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status("APPROVED")
                .build();
        
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishPaymentProcessed(saved);
        
        log.info("Payment processed: orderId={}, paymentId={}, amount={}", 
                orderId, saved.getId(), amount);
        
        return saved;
    }
}
