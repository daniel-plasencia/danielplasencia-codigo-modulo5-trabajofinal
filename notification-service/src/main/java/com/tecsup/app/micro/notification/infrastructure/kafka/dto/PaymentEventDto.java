package com.tecsup.app.micro.notification.infrastructure.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {
    private Long orderId;
    private String status;
    private Long paymentId;
}
