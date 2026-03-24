package dev.ecorank.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.service.EconomyService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * /pay <player> <amount> command — transfers coins between players.
 */
@SuppressWarnings("UnstableApiUsage")
public final class PayCommand {

    private PayCommand() {}

    /**
     * Creates the Brigadier command node for /pay.
     */
    public static LiteralCommandNode<CommandSourceStack> createCommand(
            EconomyService economyService, ConfigService configService) {

        return Commands.literal("pay")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String input = builder.getRemainingLowerCase();
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                String name = online.getName();
                                // Don't suggest yourself
                                if (ctx.getSource().getSender() instanceof Player sender
                                        && sender.getUniqueId().equals(online.getUniqueId())) {
                                    continue;
                                }
                                if (name.toLowerCase().startsWith(input)) {
                                    builder.suggest(name);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player sender)) {
                                        ctx.getSource().getSender().sendMessage(
                                                Component.text("This command can only be used by players.", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    String targetName = StringArgumentType.getString(ctx, "player");
                                    long amount = LongArgumentType.getLong(ctx, "amount");
                                    Player target = Bukkit.getPlayerExact(targetName);

                                    if (target == null) {
                                        sender.sendMessage(Component.text("Player not found: " + targetName, NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    if (sender.getUniqueId().equals(target.getUniqueId())) {
                                        sender.sendMessage(Component.text("You cannot pay yourself.", NamedTextColor.RED));
                                        return Command.SINGLE_SUCCESS;
                                    }

                                    boolean success = economyService.transfer(
                                            sender.getUniqueId(), target.getUniqueId(), amount);

                                    if (success) {
                                        String formatted = configService.formatAmount(amount);
                                        sender.sendMessage(Component.text()
                                                .append(Component.text("Paid ", NamedTextColor.GREEN))
                                                .append(Component.text(formatted, NamedTextColor.GOLD))
                                                .append(Component.text(" to ", NamedTextColor.GREEN))
                                                .append(Component.text(target.getName(), NamedTextColor.GOLD))
                                                .build());
                                        target.sendMessage(Component.text()
                                                .append(Component.text("Received ", NamedTextColor.GREEN))
                                                .append(Component.text(formatted, NamedTextColor.GOLD))
                                                .append(Component.text(" from ", NamedTextColor.GREEN))
                                                .append(Component.text(sender.getName(), NamedTextColor.GOLD))
                                                .build());
                                    } else {
                                        sender.sendMessage(Component.text("Transfer failed. You may have insufficient funds.", NamedTextColor.RED));
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                )
                .build();
    }
}
