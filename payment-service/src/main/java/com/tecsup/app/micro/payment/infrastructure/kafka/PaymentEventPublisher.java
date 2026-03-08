package com.tecsup.app.micro.payment.infrastructure.kafka;

import com.tecsup.app.micro.payment.domain.model.Payment;
import com.tecsup.app.micro.payment.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.payment.infrastructure.kafka.dto.PaymentProcessedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishPaymentProcessed(Payment payment) {
        PaymentProcessedEvent event = new PaymentProcessedEvent(
                payment.getOrderId(),
                "APPROVED",
                payment.getId()
        );
        kafkaTemplate.send(KafkaConfig.PAYMENTS_EVENTS_TOPIC, "payment-" + payment.getId(), event);
        log.info("Published PaymentProcessedEvent for payment id: {}", payment.getId());
    }
}
