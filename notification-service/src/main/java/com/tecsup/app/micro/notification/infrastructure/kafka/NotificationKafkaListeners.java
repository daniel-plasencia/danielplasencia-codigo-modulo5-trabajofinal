package com.tecsup.app.micro.notification.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.app.micro.notification.application.usecase.CreateNotificationUseCase;
import com.tecsup.app.micro.notification.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.notification.infrastructure.kafka.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaListeners {
    
    private final CreateNotificationUseCase createNotificationUseCase;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = KafkaConfig.ORDERS_EVENTS_TOPIC, groupId = "notification-service-group")
    public void handleOrderEvent(String message) {
        try {
            OrderEventDto event = objectMapper.readValue(message, OrderEventDto.class);
            createNotificationUseCase.execute(
                    event.getUserId(),
                    "Tu pedido #" + event.getOrderId() + " ha sido creado",
                    "ORDER_CREATED"
            );
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = KafkaConfig.PAYMENTS_EVENTS_TOPIC, groupId = "notification-service-group")
    public void handlePaymentEvent(String message) {
        try {
            PaymentEventDto event = objectMapper.readValue(message, PaymentEventDto.class);
            if ("APPROVED".equals(event.getStatus())) {
                createNotificationUseCase.execute(
                        null,
                        "Pago aprobado para pedido #" + event.getOrderId(),
                        "PAYMENT_APPROVED"
                );
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
    
    @KafkaListener(topics = KafkaConfig.DELIVERIES_EVENTS_TOPIC, groupId = "notification-service-group")
    public void handleDeliveryEvent(String message) {
        try {
            DeliveryEventDto event = objectMapper.readValue(message, DeliveryEventDto.class);
            createNotificationUseCase.execute(
                    null,
                    "Tu pedido #" + event.getOrderId() + " está en camino",
                    "DELIVERY_STARTED"
            );
        } catch (Exception e) {
            log.error("Error processing delivery event: {}", e.getMessage(), e);
        }
    }
}
