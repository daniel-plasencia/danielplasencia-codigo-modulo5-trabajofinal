package com.tecsup.app.micro.order.presentation.controller;

import com.tecsup.app.micro.order.application.usecase.CreateOrderUseCase;
import com.tecsup.app.micro.order.application.usecase.GetOrderByIdUseCase;
import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.infrastructure.config.JwtTokenProvider;
import com.tecsup.app.micro.order.presentation.dto.CreateOrderRequest;
import com.tecsup.app.micro.order.presentation.dto.OrderItemRequest;
import com.tecsup.app.micro.order.presentation.dto.OrderItemResponse;
import com.tecsup.app.micro.order.presentation.dto.OrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        String token = extractToken(httpRequest);
        String email = jwtTokenProvider.getEmailFromToken(token);
        Long userId = 1L; // Simplificado: usar ID fijo por ahora
        
        Order order = Order.builder()
                .userId(userId)
                .items(request.getItems().stream()
                        .map(this::toOrderItem)
                        .collect(Collectors.toList()))
                .build();
        
        Order created = createOrderUseCase.execute(order);
        return ResponseEntity.ok(toResponse(created));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = getOrderByIdUseCase.execute(id);
        return ResponseEntity.ok(toResponse(order));
    }
    
    private OrderItem toOrderItem(OrderItemRequest request) {
        return OrderItem.builder()
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();
    }
    
    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
    
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
