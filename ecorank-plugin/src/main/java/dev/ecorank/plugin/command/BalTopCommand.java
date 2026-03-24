package dev.ecorank.plugin.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.ecorank.plugin.config.ConfigService;
import dev.ecorank.plugin.model.PlayerAccount;
import dev.ecorank.plugin.service.EconomyService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

/**
 * /baltop [page] command — paginated balance leaderboard.
 */
@SuppressWarnings("UnstableApiUsage")
public final class BalTopCommand {

    private static final int ENTRIES_PER_PAGE = 10;

    private BalTopCommand() {}

    /**
     * Creates the Brigadier command node for /baltop.
     */
    public static LiteralCommandNode<CommandSourceStack> createCommand(
            EconomyService economyService, ConfigService configService) {

        return Commands.literal("baltop")
                // /baltop — page 1
                .executes(ctx -> {
                    showLeaderboard(ctx.getSource(), economyService, configService, 1);
                    return Command.SINGLE_SUCCESS;
                })
                // /baltop <page>
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            int page = IntegerArgumentType.getInteger(ctx, "page");
                            showLeaderboard(ctx.getSource(), economyService, configService, page);
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
    }

    private static void showLeaderboard(CommandSourceStack source,
                                         EconomyService economyService,
                                         ConfigService configService,
                                         int page) {
        // Fetch enough entries for the requested page
        int maxEntries = page * ENTRIES_PER_PAGE;
        List<PlayerAccount> allTop = economyService.getTopBalances(maxEntries);

        int totalEntries = allTop.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalEntries / ENTRIES_PER_PAGE));

        if (page > totalPages && totalEntries > 0) {
            source.getSender().sendMessage(
                    Component.text("Page " + page + " does not exist. Max page: " + totalPages, NamedTextColor.RED));
            return;
        }

        int startIndex = (page - 1) * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, totalEntries);

        // Header
        source.getSender().sendMessage(Component.text()
                .append(Component.text("===== ", NamedTextColor.GOLD))
                .append(Component.text("Balance Top", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                .append(Component.text(" (Page " + page + "/" + totalPages + ") ", NamedTextColor.GRAY))
                .append(Component.text("=====", NamedTextColor.GOLD))
                .build());

        if (totalEntries == 0) {
            source.getSender().sendMessage(
                    Component.text("No players found.", NamedTextColor.GRAY));
            return;
        }

        // Entries
        for (int i = startIndex; i < endIndex; i++) {
            PlayerAccount account = allTop.get(i);
            int rank = i + 1;

            NamedTextColor rankColor = switch (rank) {
                case 1 -> NamedTextColor.GOLD;
                case 2 -> NamedTextColor.GRAY;
                case 3 -> NamedTextColor.RED;
                default -> NamedTextColor.WHITE;
            };

            source.getSender().sendMessage(Component.text()
                    .append(Component.text("#" + rank + " ", rankColor).decorate(TextDecoration.BOLD))
                    .append(Component.text(account.playerName(), NamedTextColor.AQUA))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(configService.formatAmount(account.balance()), NamedTextColor.GREEN))
                    .build());
        }
    }
}
