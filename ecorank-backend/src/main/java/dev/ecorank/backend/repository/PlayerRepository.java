package dev.ecorank.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.ecorank.backend.entity.Player;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByMinecraftUuid(UUID minecraftUuid);

    @Query("SELECT p FROM Player p WHERE LOWER(p.username) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Player> searchByUsername(@Param("query") String query);
}
