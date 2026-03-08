package com.tecsup.app.micro.notification.infrastructure.kafka;

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
    
    @KafkaListener(topics = KafkaConfig.ORDERS_EVENTS_TOPIC, groupId = "notification-service-group")
    public void handleOrderEvent(OrderEventDto event) {
        createNotificationUseCase.execute(
                event.getUserId(),
                "Tu pedido #" + event.getOrderId() + " ha sido creado",
                "ORDER_CREATED"
        );
    }
    
    @KafkaListener(topics = KafkaConfig.PAYMENTS_EVENTS_TOPIC, groupId = "notification-service-group")
    public void handlePaymentEvent(PaymentEventDto event) {
        if ("APPROVED".equals(event.getStatus())) {
            createNotificationUseCase.execute(
                    null,
                    "Pago aprobado para pedido #" + event.getOrderId(),
                    "PAYMENT_APPROVED"
            );
        }
    }
    
    @KafkaListener(topics = KafkaConfig.DELIVERIES_EVENTS_TOPIC, groupId = "notification-service-group")
    public void handleDeliveryEvent(DeliveryEventDto event) {
        createNotificationUseCase.execute(
                null,
                "Tu pedido #" + event.getOrderId() + " está en camino",
                "DELIVERY_STARTED"
        );
    }
}
