package dev.ecorank.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.PlayerAccount;
import dev.ecorank.plugin.service.EconomyService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * /balance [player] command — shows own or another player's balance.
 * Aliases: bal, money
 */
@SuppressWarnings("UnstableApiUsage")
public final class BalanceCommand {

    private BalanceCommand() {}

    /**
     * Creates the Brigadier command node for /balance.
     */
    public static LiteralCommandNode<CommandSourceStack> createCommand(
            EconomyService economyService, ConfigService configService) {

        return Commands.literal("balance")
                // /balance — show own balance
                .executes(ctx -> {
                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage(
                                Component.text("This command can only be used by players.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    long balance = economyService.getBalance(player.getUniqueId());
                    player.sendMessage(Component.text()
                            .append(Component.text("Balance: ", NamedTextColor.GOLD))
                            .append(Component.text(configService.formatAmount(balance), NamedTextColor.GREEN))
                            .build());
                    return Command.SINGLE_SUCCESS;
                })
                // /balance <player> — show another player's balance
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String input = builder.getRemainingLowerCase();
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                if (online.getName().toLowerCase().startsWith(input)) {
                                    builder.suggest(online.getName());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            String targetName = StringArgumentType.getString(ctx, "player");
                            Player target = Bukkit.getPlayerExact(targetName);

                            if (target == null) {
                                ctx.getSource().getSender().sendMessage(
                                        Component.text("Player not found: " + targetName, NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }

                            Optional<PlayerAccount> account = economyService.getAccount(target.getUniqueId());
                            if (account.isEmpty()) {
                                ctx.getSource().getSender().sendMessage(
                                        Component.text("No account found for " + targetName, NamedTextColor.RED));
                                return Command.SINGLE_SUCCESS;
                            }

                            ctx.getSource().getSender().sendMessage(Component.text()
                                    .append(Component.text(target.getName() + "'s Balance: ", NamedTextColor.GOLD))
                                    .append(Component.text(configService.formatAmount(account.get().balance()), NamedTextColor.GREEN))
                                    .build());
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }
}
