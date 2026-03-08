package com.tecsup.app.micro.delivery.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {
    private Long id;
    private Long orderId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
