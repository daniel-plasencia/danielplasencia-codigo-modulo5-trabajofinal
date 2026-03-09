package com.tecsup.app.micro.order.presentation.controller;

import com.tecsup.app.micro.order.application.usecase.CreateOrderUseCase;
import com.tecsup.app.micro.order.application.usecase.GetOrderByIdUseCase;
import com.tecsup.app.micro.order.domain.model.Order;
import com.tecsup.app.micro.order.domain.model.OrderItem;
import com.tecsup.app.micro.order.infrastructure.client.ProductClient;
import com.tecsup.app.micro.order.infrastructure.client.dto.ProductDto;
import com.tecsup.app.micro.order.infrastructure.config.JwtTokenProvider;
import com.tecsup.app.micro.order.presentation.dto.CreateOrderRequest;
import com.tecsup.app.micro.order.presentation.dto.OrderItemRequest;
import com.tecsup.app.micro.order.presentation.dto.OrderItemResponse;
import com.tecsup.app.micro.order.presentation.dto.OrderResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderByIdUseCase getOrderByIdUseCase;
    private final JwtTokenProvider jwtTokenProvider;
    private final ProductClient productClient;
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest httpRequest) {
        
        String token = extractToken(httpRequest);
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Creating order for userId: {} with {} items", userId, request.getItems().size());

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            ProductDto product = productClient.getProductById(itemReq.getProductId(), token);
            if (product == null) {
                throw new IllegalArgumentException(
                        "Producto no encontrado o servicio no disponible: " + itemReq.getProductId());
            }
            if (!product.isAvailable() || product.getStock() < itemReq.getQuantity()) {
                throw new IllegalArgumentException(
                        "Producto sin stock suficiente: " + product.getName());
            }

            items.add(OrderItem.builder()
                    .productId(product.getId())
                    .quantity(itemReq.getQuantity())
                    .price(product.getPrice())
                    .build());
        }
        
        Order order = Order.builder()
                .userId(userId)
                .items(items)
                .build();
        
        Order created = createOrderUseCase.execute(order);
        return ResponseEntity.ok(toResponse(created));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        Order order = getOrderByIdUseCase.execute(id);
        return ResponseEntity.ok(toResponse(order));
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service running with Clean Architecture!");
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
