package dev.ecorank.backend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ecorank.backend.entity.Player;
import dev.ecorank.backend.exception.ResourceNotFoundException;
import dev.ecorank.backend.repository.PlayerRepository;

@Service
public class PlayerService {

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final PlayerRepository playerRepository;
    private final ObjectMapper objectMapper;

    public PlayerService(PlayerRepository playerRepository, ObjectMapper objectMapper) {
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
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

    /**
     * Resolves a Minecraft player UUID from their username via the Mojang API.
     *
     * @param playerName the Minecraft username
     * @return the player's UUID, or null if resolution failed
     */
    public UUID resolveMinecraftUuid(String playerName) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + playerName))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String rawUuid = json.get("id").asText();
                String formatted = rawUuid.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(formatted);
            }
            log.warn("Mojang API returned status {} for player {}", response.statusCode(), playerName);
        } catch (Exception e) {
            log.warn("Failed to resolve Minecraft UUID for {}: {}", playerName, e.getMessage());
        }
        return null;
    }
}
