package com.tecsup.app.micro.order.infrastructure.kafka;

import com.tecsup.app.micro.order.application.usecase.UpdateOrderStatusUseCase;
import com.tecsup.app.micro.order.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.order.infrastructure.kafka.dto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventKafkaListener {
    
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    
    @KafkaListener(topics = KafkaConfig.PAYMENTS_EVENTS_TOPIC, groupId = "order-service-group")
    public void handlePaymentEvent(PaymentEventDto event) {
        log.info("Received payment event: orderId={}, status={}", event.getOrderId(), event.getStatus());
        
        if ("APPROVED".equals(event.getStatus())) {
            updateOrderStatusUseCase.execute(event.getOrderId(), "PAID");
        } else if ("REJECTED".equals(event.getStatus())) {
            updateOrderStatusUseCase.execute(event.getOrderId(), "PAYMENT_FAILED");
        }
    }
}
