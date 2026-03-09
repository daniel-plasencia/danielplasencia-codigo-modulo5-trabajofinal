package com.tecsup.app.micro.order.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.app.micro.order.application.usecase.UpdateOrderStatusUseCase;
import com.tecsup.app.micro.order.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.order.infrastructure.kafka.dto.DeliveryEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventKafkaListener {

    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaConfig.DELIVERIES_EVENTS_TOPIC, groupId = "order-service-group")
    public void handleDeliveryEvent(String message) {
        try {
            DeliveryEventDto event = objectMapper.readValue(message, DeliveryEventDto.class);
            log.info("Received delivery event: orderId={}, status={}", event.getOrderId(), event.getStatus());

            if ("DELIVERED".equals(event.getStatus())) {
                updateOrderStatusUseCase.execute(event.getOrderId(), "DELIVERED");
            }
        } catch (Exception e) {
            log.error("Error processing delivery event: {}", e.getMessage(), e);
        }
    }
}
