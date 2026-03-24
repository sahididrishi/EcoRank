package dev.ecorank.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ecorank.backend.dto.request.CreateProductRequest;
import dev.ecorank.backend.dto.request.UpdateProductRequest;
import dev.ecorank.backend.entity.Product;
import dev.ecorank.backend.exception.DuplicateResourceException;
import dev.ecorank.backend.exception.ResourceNotFoundException;
import dev.ecorank.backend.mapper.ProductMapper;
import dev.ecorank.backend.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    @Transactional(readOnly = true)
    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrueOrderBySortOrder();
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional
    public Product createProduct(CreateProductRequest request) {
        if (productRepository.findBySlug(request.slug()).isPresent()) {
            throw new DuplicateResourceException("Product with slug '" + request.slug() + "' already exists");
        }
        Product product = productMapper.toEntity(request);
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.slug() != null) {
            // Check for duplicate slug if changing
            productRepository.findBySlug(request.slug())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("Product with slug '" + request.slug() + "' already exists");
                    });
            product.setSlug(request.slug());
        }
        if (request.priceCents() != null) {
            product.setPriceCents(request.priceCents());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.rankGroup() != null) {
            product.setRankGroup(request.rankGroup());
        }
        if (request.category() != null) {
            product.setCategory(request.category());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        if (request.sortOrder() != null) {
            product.setSortOrder(request.sortOrder());
        }

        return productRepository.save(product);
    }

    @Transactional
    public Product toggleProductActive(Long id, boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(active);
        return productRepository.save(product);
    }
}
