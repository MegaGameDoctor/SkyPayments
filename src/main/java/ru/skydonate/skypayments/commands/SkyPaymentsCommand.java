package ru.skydonate.skypayments.commands;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.config.Config;
import ru.skydonate.skypayments.config.Placeholder;
import ru.skydonate.skypayments.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class SkyPaymentsCommand implements CommandExecutor, TabExecutor {
    private final SkyPayments plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        Config cfg = plugin.getMainConfig();
        Utils utils = plugin.getUtils();
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "base64":
                    if (commandSender instanceof Player) {
                        Player player = (Player) commandSender;
                        if (args.length > 1) {
                            if (utils.getNmsUtils().hasAvailableSlots(player, 1)) {
                                try {
                                    player.getInventory().addItem(utils.readObjectFromBase64(plugin.getDatabaseManager().getCachedBase64Item(Integer.parseInt(args[1])), ItemStack.class));
                                    cfg.getMessage_mainCmdDecodeSuccess().display(player);
                                } catch (Exception ignored) {
                                    cfg.getMessage_failed().display(player);
                                }
                            } else {
                                cfg.getMessage_freeInventory().display(player);
                            }
                        } else {
                            ItemStack result = utils.getNmsUtils().getItemFromHand(player);
                            if (result != null) {
                                TextComponent answer = cfg.getMessage_mainCmdEncodeAnswer();

                                answer.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, String.valueOf(plugin.getDatabaseManager().saveBase64ItemWithCache(utils.writeObjectInBase64(result)))));
                                player.spigot().sendMessage(answer);
                            } else {
                                cfg.getMessage_mainCmdEncodeFail().display(player);
                            }
                        }
                    }
                    return true;
                default:
                    cfg.getMessage_mainCmdHelp().display(commandSender, new Placeholder("%cmd%", s));
            }
        } else {
            cfg.getMessage_mainCmdHelp().display(commandSender, new Placeholder("%cmd%", s));
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String ss, @NotNull String[] args) {
        List<String> list = Collections.singletonList("base64");
        String input = args[0].toLowerCase();


        List<String> completions = null;
        for (String s : list) {
            if (s.startsWith(input)) {
                if (completions == null) {
                    completions = new ArrayList<>();
                }
                completions.add(s);
            }
        }

        if (completions != null)
            Collections.sort(completions);

        return completions;
    }
}