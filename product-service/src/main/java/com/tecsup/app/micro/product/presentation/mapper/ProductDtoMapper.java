package com.tecsup.app.micro.product.presentation.mapper;

import com.tecsup.app.micro.product.domain.model.Product;
import com.tecsup.app.micro.product.infrastructure.client.mapper.UserDtoMapper;
import com.tecsup.app.micro.product.presentation.dto.CreateProductRequest;
import com.tecsup.app.micro.product.presentation.dto.ProductResponse;
import com.tecsup.app.micro.product.presentation.dto.UpdateProductRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper entre DTOs de presentación y modelo de dominio usando MapStruct -- --
 */
@Mapper(componentModel = "spring", uses = {UserDtoMapper.class})
public interface ProductDtoMapper {

    /**
     * Convierte CreateProductRequest a Product de dominio
     */
    Product toDomain(CreateProductRequest request);
    
    /**
     * Convierte UpdateProductRequest a Product de dominio
     */
    Product toDomain(UpdateProductRequest request);
    
    /**
     * Convierte Product de dominio a ProductResponse
     */
    @Mapping(target = "available", expression = "java(product.isAvailable())")
    ProductResponse toResponse(Product product);
    
    /**
     * Convierte lista de Products a lista de ProductResponse
     * Con el mismo mapeo para cada elemento de la lista
     */
    List<ProductResponse> toResponseList(List<Product> products);
}
