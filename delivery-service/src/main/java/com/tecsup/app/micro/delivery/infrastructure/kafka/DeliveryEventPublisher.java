package com.tecsup.app.micro.delivery.infrastructure.kafka;

import com.tecsup.app.micro.delivery.domain.model.Delivery;
import com.tecsup.app.micro.delivery.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.delivery.infrastructure.kafka.dto.DeliveryStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishDeliveryStarted(Delivery delivery) {
        DeliveryStartedEvent event = new DeliveryStartedEvent(
                delivery.getOrderId(),
                delivery.getStatus(),
                delivery.getId()
        );
        kafkaTemplate.send(KafkaConfig.DELIVERIES_EVENTS_TOPIC, "delivery-" + delivery.getId(), event);
        log.info("Published DeliveryStartedEvent for delivery id: {}", delivery.getId());
    }
}
