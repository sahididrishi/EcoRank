package dev.ecorank.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.ecorank.backend.entity.AdminUser;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsername(String username);
}
