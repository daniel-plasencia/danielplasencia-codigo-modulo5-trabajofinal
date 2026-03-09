package com.tecsup.app.micro.delivery.domain.repository;

import com.tecsup.app.micro.delivery.domain.model.Delivery;

import java.util.List;
import java.util.Optional;

public interface DeliveryRepository {
    Delivery save(Delivery delivery);
    Optional<Delivery> findById(Long id);
    Optional<Delivery> findByOrderId(Long orderId);
    List<Delivery> findAll();
}
