package com.tecsup.app.micro.order.infrastructure.client;

import com.tecsup.app.micro.order.infrastructure.client.dto.ProductDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Retry(name = "productService")
    public ProductDto getProductById(Long productId, String jwtToken) {
        log.info("Calling Product Service to get product with id: {}", productId);

        String url = this.productServiceUrl + "/api/products/" + productId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtToken != null && !jwtToken.isEmpty()) {
            headers.setBearerAuth(jwtToken);
        }

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ProductDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, ProductDto.class
            );
            log.info("Product retrieved successfully: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("Error calling Product Service: {}", e.getMessage());
            throw new RuntimeException("Error calling Product Service: " + e.getMessage());
        }
    }

    public ProductDto getProductFallback(Long productId, String jwtToken, Throwable throwable) {
        log.warn("FALLBACK: Product Service no disponible para productId: {}. Razón: {}",
                productId, throwable.getMessage());
        return null;
    }
}
