package dev.ecorank.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.ecorank.backend.entity.Order;
import dev.ecorank.backend.entity.OrderStatus;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"player", "product"})
    Optional<Order> findById(Long id);

    Optional<Order> findByIdempotencyKey(UUID idempotencyKey);

    Optional<Order> findByProviderPaymentId(String providerPaymentId);

    @EntityGraph(attributePaths = {"player", "product"})
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.amountCents), 0) FROM Order o WHERE o.status IN :statuses")
    long sumAmountCentsByStatusIn(@Param("statuses") List<OrderStatus> statuses);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT o FROM Order o JOIN FETCH o.player JOIN FETCH o.product WHERE o.player.minecraftUuid = :uuid ORDER BY o.createdAt DESC")
    List<Order> findByPlayerUuid(@Param("uuid") UUID uuid);
}
