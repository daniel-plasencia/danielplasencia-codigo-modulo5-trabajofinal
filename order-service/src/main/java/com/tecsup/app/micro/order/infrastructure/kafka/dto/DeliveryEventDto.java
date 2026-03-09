package com.tecsup.app.micro.order.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryEventDto {
    private Long orderId;
    private String status;
    private Long deliveryId;
}
