package com.tecsup.app.micro.order.domain.repository;

import com.tecsup.app.micro.order.domain.model.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByUserId(Long userId);
    List<Order> findAll();
}
