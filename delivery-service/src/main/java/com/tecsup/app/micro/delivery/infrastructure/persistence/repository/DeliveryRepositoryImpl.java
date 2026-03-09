package com.tecsup.app.micro.delivery.infrastructure.persistence.repository;

import com.tecsup.app.micro.delivery.domain.model.Delivery;
import com.tecsup.app.micro.delivery.domain.repository.DeliveryRepository;
import com.tecsup.app.micro.delivery.infrastructure.persistence.entity.DeliveryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class DeliveryRepositoryImpl implements DeliveryRepository {
    
    private final JpaDeliveryRepository jpaDeliveryRepository;
    
    @Override
    public Delivery save(Delivery delivery) {
        DeliveryEntity entity = toEntity(delivery);
        DeliveryEntity saved = jpaDeliveryRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<Delivery> findById(Long id) {
        return jpaDeliveryRepository.findById(id)
                .map(this::toDomain);
    }
    
    @Override
    public Optional<Delivery> findByOrderId(Long orderId) {
        return jpaDeliveryRepository.findByOrderId(orderId)
                .map(this::toDomain);
    }

    @Override
    public List<Delivery> findAll() {
        return jpaDeliveryRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    private Delivery toDomain(DeliveryEntity entity) {
        return Delivery.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private DeliveryEntity toEntity(Delivery delivery) {
        return DeliveryEntity.builder()
                .id(delivery.getId())
                .orderId(delivery.getOrderId())
                .status(delivery.getStatus())
                .build();
    }
}
