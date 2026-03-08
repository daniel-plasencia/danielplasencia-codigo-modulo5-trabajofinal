package com.tecsup.app.micro.order.infrastructure.persistence.repository;

import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.domain.repository.OrderRepository;
import com.tecsup.app.micro.order.infrastructure.persistence.entity.OrderEntity;
import com.tecsup.app.micro.order.infrastructure.persistence.entity.OrderItemEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {
    
    private final JpaOrderRepository jpaOrderRepository;
    private final JpaOrderItemRepository jpaOrderItemRepository;
    
    @Override
    public Order save(Order order) {
        OrderEntity entity = toEntity(order);
        OrderEntity saved = jpaOrderRepository.save(entity);
        
        if (order.getItems() != null) {
            List<OrderItemEntity> items = order.getItems().stream()
                    .map(item -> toItemEntity(item, saved.getId()))
                    .collect(Collectors.toList());
            jpaOrderItemRepository.saveAll(items);
        }
        
        return toDomain(saved);
    }
    
    @Override
    public Optional<Order> findById(Long id) {
        return jpaOrderRepository.findById(id)
                .map(this::toDomain);
    }
    
    @Override
    public List<Order> findByUserId(Long userId) {
        return jpaOrderRepository.findByUserId(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Order> findAll() {
        return jpaOrderRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
    
    private Order toDomain(OrderEntity entity) {
        List<OrderItem> items = jpaOrderItemRepository.findByOrderId(entity.getId()).stream()
                .map(this::toItemDomain)
                .collect(Collectors.toList());
        
        return Order.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .items(items)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    private OrderEntity toEntity(Order order) {
        return OrderEntity.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus() != null ? order.getStatus() : "PENDING")
                .totalAmount(order.getTotalAmount())
                .build();
    }
    
    private OrderItem toItemDomain(OrderItemEntity entity) {
        return OrderItem.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .productId(entity.getProductId())
                .quantity(entity.getQuantity())
                .price(entity.getPrice())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    
    private OrderItemEntity toItemEntity(OrderItem item, Long orderId) {
        return OrderItemEntity.builder()
                .orderId(orderId)
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
