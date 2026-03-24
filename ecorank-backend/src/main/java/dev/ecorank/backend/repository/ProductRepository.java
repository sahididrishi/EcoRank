package dev.ecorank.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.ecorank.backend.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    List<Product> findByActiveTrueOrderBySortOrder();
}
