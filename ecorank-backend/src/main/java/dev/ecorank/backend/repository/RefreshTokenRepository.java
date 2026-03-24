package dev.ecorank.backend.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.ecorank.backend.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.adminId = :adminId AND r.revoked = false")
    void revokeAllByAdminId(@Param("adminId") Long adminId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
    int deleteExpiredOrRevoked(@Param("now") Instant now);
}
