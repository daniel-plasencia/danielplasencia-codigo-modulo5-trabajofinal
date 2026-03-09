package com.tecsup.app.micro.payment.application.usecase;

import com.tecsup.app.micro.payment.domain.model.Payment;
import com.tecsup.app.micro.payment.domain.repository.PaymentRepository;
import com.tecsup.app.micro.payment.infrastructure.kafka.PaymentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovePaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;

    public Payment execute(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No existe un pago para la orden: " + orderId));

        if (!"PENDING".equals(payment.getStatus())) {
            throw new IllegalStateException("El pago ya fue procesado. Status actual: " + payment.getStatus());
        }

        payment.setStatus("APPROVED");
        Payment saved = paymentRepository.save(payment);
        eventPublisher.publishPaymentProcessed(saved);

        log.info("Payment APPROVED: orderId={}, paymentId={}", orderId, saved.getId());
        return saved;
    }
}
