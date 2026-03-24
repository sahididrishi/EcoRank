package dev.ecorank.backend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ecorank.backend.entity.Player;
import dev.ecorank.backend.exception.ResourceNotFoundException;
import dev.ecorank.backend.repository.PlayerRepository;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public Player getOrCreatePlayer(UUID minecraftUuid, String username) {
        return playerRepository.findByMinecraftUuid(minecraftUuid)
                .map(existing -> {
                    if (username != null && !username.equals(existing.getUsername())) {
                        existing.setUsername(username);
                        return playerRepository.save(existing);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    Player player = new Player(minecraftUuid, username);
                    return playerRepository.save(player);
                });
    }

    @Transactional(readOnly = true)
    public Player getPlayerByUuid(UUID minecraftUuid) {
        return playerRepository.findByMinecraftUuid(minecraftUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Player", minecraftUuid));
    }

    @Transactional(readOnly = true)
    public long getTotalPlayers() {
        return playerRepository.count();
    }

    @Transactional(readOnly = true)
    public List<Player> searchPlayers(String query) {
        if (query == null || query.isBlank()) {
            return playerRepository.findAll();
        }
        return playerRepository.searchByUsername(query);
    }
}
