package dev.ecorank.backend.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import dev.ecorank.backend.dto.response.OrderResponse;
import dev.ecorank.backend.dto.response.PendingOrderResponse;
import dev.ecorank.backend.entity.Order;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "player.username", target = "playerName")
    @Mapping(source = "player.minecraftUuid", target = "playerUuid")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.slug", target = "productSlug")
    @Mapping(target = "status", expression = "java(order.getStatus().name())")
    OrderResponse toResponse(Order order);

    @Mapping(target = "orderId", expression = "java(String.valueOf(order.getId()))")
    @Mapping(source = "player.minecraftUuid", target = "playerUuid")
    @Mapping(source = "product.slug", target = "productSlug")
    @Mapping(source = "product.rankGroup", target = "rankGroup")
    @Mapping(target = "action", constant = "GRANT_RANK")
    PendingOrderResponse toPendingResponse(Order order);
}
