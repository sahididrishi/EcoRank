package dev.ecorank.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.TransactionType;
import dev.ecorank.plugin.service.EconomyService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * /eco give|take|set <player> <amount> — admin economy management.
 * Requires permission: ecorank.admin
 */
@SuppressWarnings("UnstableApiUsage")
public final class EcoCommand {

    private EcoCommand() {}

    /**
     * Creates the Brigadier command node for /eco.
     */
    public static LiteralCommandNode<CommandSourceStack> createCommand(
            EconomyService economyService, ConfigService configService) {

        return Commands.literal("eco")
                .requires(source -> source.getSender().hasPermission("ecorank.admin"))
                // /eco give <player> <amount>
                .then(Commands.literal("give")
                        .then(playerArgument()
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            String targetName = StringArgumentType.getString(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            return handleGive(ctx.getSource(), economyService, configService, targetName, amount);
                                        })
                                )
                        )
                )
                // /eco take <player> <amount>
                .then(Commands.literal("take")
                        .then(playerArgument()
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            String targetName = StringArgumentType.getString(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            return handleTake(ctx.getSource(), economyService, configService, targetName, amount);
                                        })
                                )
                        )
                )
                // /eco set <player> <amount>
                .then(Commands.literal("set")
                        .then(playerArgument()
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(ctx -> {
                                            String targetName = StringArgumentType.getString(ctx, "player");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            return handleSet(ctx.getSource(), economyService, configService, targetName, amount);
                                        })
                                )
                        )
                )
                .build();
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> playerArgument() {
        return Commands.argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    String input = builder.getRemainingLowerCase();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (online.getName().toLowerCase().startsWith(input)) {
                            builder.suggest(online.getName());
                        }
                    }
                    return builder.buildFuture();
                });
    }

    private static int handleGive(CommandSourceStack source, EconomyService economyService,
                                   ConfigService configService, String targetName, long amount) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            source.getSender().sendMessage(
                    Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        boolean success = economyService.deposit(
                target.getUniqueId(), amount, TransactionType.ADMIN_GIVE,
                "Admin give by " + source.getSender().getName());

        if (success) {
            String formatted = configService.formatAmount(amount);
            source.getSender().sendMessage(Component.text()
                    .append(Component.text("Gave ", NamedTextColor.GREEN))
                    .append(Component.text(formatted, NamedTextColor.GOLD))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.GOLD))
                    .build());
            target.sendMessage(Component.text()
                    .append(Component.text("You received ", NamedTextColor.GREEN))
                    .append(Component.text(formatted, NamedTextColor.GOLD))
                    .append(Component.text(" from an admin.", NamedTextColor.GREEN))
                    .build());
        } else {
            source.getSender().sendMessage(
                    Component.text("Failed to give coins to " + targetName, NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int handleTake(CommandSourceStack source, EconomyService economyService,
                                   ConfigService configService, String targetName, long amount) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            source.getSender().sendMessage(
                    Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        boolean success = economyService.withdraw(
                target.getUniqueId(), amount, TransactionType.ADMIN_TAKE,
                "Admin take by " + source.getSender().getName());

        if (success) {
            String formatted = configService.formatAmount(amount);
            source.getSender().sendMessage(Component.text()
                    .append(Component.text("Took ", NamedTextColor.GREEN))
                    .append(Component.text(formatted, NamedTextColor.GOLD))
                    .append(Component.text(" from ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.GOLD))
                    .build());
        } else {
            source.getSender().sendMessage(
                    Component.text("Failed to take coins from " + targetName + ". Insufficient funds?", NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int handleSet(CommandSourceStack source, EconomyService economyService,
                                  ConfigService configService, String targetName, long amount) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            source.getSender().sendMessage(
                    Component.text("Player not found: " + targetName, NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }

        boolean success = economyService.setBalance(target.getUniqueId(), amount);

        if (success) {
            String formatted = configService.formatAmount(amount);
            source.getSender().sendMessage(Component.text()
                    .append(Component.text("Set ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.GOLD))
                    .append(Component.text("'s balance to ", NamedTextColor.GREEN))
                    .append(Component.text(formatted, NamedTextColor.GOLD))
                    .build());
        } else {
            source.getSender().sendMessage(
                    Component.text("Failed to set balance for " + targetName, NamedTextColor.RED));
        }

        return Command.SINGLE_SUCCESS;
    }
}
