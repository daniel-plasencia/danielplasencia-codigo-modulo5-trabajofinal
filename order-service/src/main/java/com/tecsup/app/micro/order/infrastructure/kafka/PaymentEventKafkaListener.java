package com.tecsup.app.micro.order.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = KafkaConfig.PAYMENTS_EVENTS_TOPIC, groupId = "order-service-group")
    public void handlePaymentEvent(String message) {
        try {
            PaymentEventDto event = objectMapper.readValue(message, PaymentEventDto.class);
            log.info("Received payment event: orderId={}, status={}", event.getOrderId(), event.getStatus());
            
            if ("APPROVED".equals(event.getStatus())) {
                updateOrderStatusUseCase.execute(event.getOrderId(), "PAID");
            } else if ("REJECTED".equals(event.getStatus())) {
                updateOrderStatusUseCase.execute(event.getOrderId(), "PAYMENT_FAILED");
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
}
