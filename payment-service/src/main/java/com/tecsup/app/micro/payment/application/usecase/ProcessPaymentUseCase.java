package com.tecsup.app.micro.payment.application.usecase;

import com.tecsup.app.micro.payment.domain.model.Payment;
import com.tecsup.app.micro.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {
    
    private final PaymentRepository paymentRepository;
    
    public Payment execute(Long orderId, BigDecimal amount) {
        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(amount)
                .status("PENDING")
                .build();
        
        Payment saved = paymentRepository.save(payment);
        log.info("Payment created as PENDING: orderId={}, paymentId={}, amount={}", 
                orderId, saved.getId(), amount);
        
        return saved;
    }
}
