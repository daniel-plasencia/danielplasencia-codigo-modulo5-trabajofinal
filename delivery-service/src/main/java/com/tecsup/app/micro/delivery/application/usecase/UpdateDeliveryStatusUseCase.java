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
public class UpdateDeliveryStatusUseCase {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventPublisher eventPublisher;

    public Delivery execute(Long deliveryId, String newStatus) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada: " + deliveryId));

        delivery.setStatus(newStatus);
        Delivery saved = deliveryRepository.save(delivery);

        eventPublisher.publishDeliveryEvent(saved);
        log.info("Delivery updated: deliveryId={}, orderId={}, status={}", 
                saved.getId(), saved.getOrderId(), newStatus);

        return saved;
    }
}
