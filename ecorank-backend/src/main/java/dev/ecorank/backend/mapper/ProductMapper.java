package dev.ecorank.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import dev.ecorank.backend.dto.request.CreateProductRequest;
import dev.ecorank.backend.dto.response.ProductResponse;
import dev.ecorank.backend.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "sortOrder", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Product toEntity(CreateProductRequest request);
}
