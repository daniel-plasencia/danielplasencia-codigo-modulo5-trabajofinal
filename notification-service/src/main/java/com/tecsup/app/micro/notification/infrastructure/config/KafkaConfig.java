package com.tecsup.app.micro.notification.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConfig {
    public static final String ORDERS_EVENTS_TOPIC = "orders.events";
    public static final String PAYMENTS_EVENTS_TOPIC = "payments.events";
    public static final String DELIVERIES_EVENTS_TOPIC = "deliveries.events";
    
    @Bean
    public NewTopic ordersEventsTopic() {
        return new NewTopic(ORDERS_EVENTS_TOPIC, 1, (short) 1);
    }
    
    @Bean
    public NewTopic paymentsEventsTopic() {
        return new NewTopic(PAYMENTS_EVENTS_TOPIC, 1, (short) 1);
    }
    
    @Bean
    public NewTopic deliveriesEventsTopic() {
        return new NewTopic(DELIVERIES_EVENTS_TOPIC, 1, (short) 1);
    }
}
