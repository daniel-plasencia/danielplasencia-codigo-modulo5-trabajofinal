package com.tecsup.app.micro.order.infrastructure.kafka;

import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.infrastructure.config.KafkaConfig;
import com.tecsup.app.micro.order.infrastructure.kafka.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getUserId(),
                order.getTotalAmount()
        );
        kafkaTemplate.send(KafkaConfig.ORDERS_EVENTS_TOPIC, "order-" + order.getId(), event);
        log.info("Published OrderCreatedEvent for order id: {}", order.getId());
    }
}
