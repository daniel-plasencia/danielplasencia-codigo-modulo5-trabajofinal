package com.tecsup.app.micro.delivery.application.usecase;

import com.tecsup.app.micro.delivery.domain.model.Delivery;
import com.tecsup.app.micro.delivery.domain.repository.DeliveryRepository;
import com.tecsup.app.micro.delivery.infrastructure.kafka.DeliveryEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateDeliveryUseCase {
    
    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventPublisher eventPublisher;
    
    public Delivery execute(Long orderId) {
        Delivery delivery = Delivery.builder()
                .orderId(orderId)
                .status("IN_TRANSIT")
                .build();
        
        Delivery saved = deliveryRepository.save(delivery);
        eventPublisher.publishDeliveryStarted(saved);
        
        log.info("Delivery created: orderId={}, deliveryId={}", orderId, saved.getId());
        return saved;
    }
}
