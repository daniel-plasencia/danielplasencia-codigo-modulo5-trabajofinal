package com.tecsup.app.micro.delivery.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecsup.app.micro.delivery.application.usecase.CreateDeliveryUseCase;
import com.tecsup.app.micro.delivery.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.delivery.infrastructure.kafka.dto.PaymentEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventKafkaListener {
    
    private final CreateDeliveryUseCase createDeliveryUseCase;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(topics = KafkaConfig.PAYMENTS_EVENTS_TOPIC, groupId = "delivery-service-group")
    public void handlePaymentEvent(String message) {
        try {
            PaymentEventDto event = objectMapper.readValue(message, PaymentEventDto.class);
            if ("APPROVED".equals(event.getStatus())) {
                log.info("Creating delivery for order: {}", event.getOrderId());
                createDeliveryUseCase.execute(event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Error processing payment event: {}", e.getMessage(), e);
        }
    }
}
