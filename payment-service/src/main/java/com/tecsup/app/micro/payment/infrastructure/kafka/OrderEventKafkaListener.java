package com.tecsup.app.micro.payment.infrastructure.kafka;

import com.tecsup.app.micro.payment.application.usecase.ProcessPaymentUseCase;
import com.tecsup.app.micro.payment.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.payment.infrastructure.kafka.dto.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventKafkaListener {
    
    private final ProcessPaymentUseCase processPaymentUseCase;
    
    @KafkaListener(topics = KafkaConfig.ORDERS_EVENTS_TOPIC, groupId = "payment-service-group")
    public void handleOrderEvent(OrderEventDto event) {
        log.info("Received order event: orderId={}, amount={}", event.getOrderId(), event.getAmount());
        processPaymentUseCase.execute(event.getOrderId(), event.getAmount());
    }
}
