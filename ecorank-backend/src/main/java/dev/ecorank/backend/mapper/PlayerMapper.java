package dev.ecorank.backend.mapper;

import org.mapstruct.Mapper;

import dev.ecorank.backend.dto.response.PlayerResponse;
import dev.ecorank.backend.entity.Player;

@Mapper(componentModel = "spring")
public interface PlayerMapper {

    PlayerResponse toResponse(Player player);
}
