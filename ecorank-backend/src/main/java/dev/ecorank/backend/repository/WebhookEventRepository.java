package dev.ecorank.backend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.ecorank.backend.entity.WebhookEvent;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    boolean existsByEventId(String eventId);

    Optional<WebhookEvent> findByEventId(String eventId);

    List<WebhookEvent> findByProcessedFalseAndCreatedAtBefore(Instant before);

    @Modifying
    @Query("DELETE FROM WebhookEvent w WHERE w.processed = true AND w.processedAt < :before")
    int deleteProcessedBefore(@Param("before") Instant before);
}
