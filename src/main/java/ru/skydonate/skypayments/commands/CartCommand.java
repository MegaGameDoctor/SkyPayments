package ru.skydonate.skypayments.commands;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.cart.CartGUI;

@RequiredArgsConstructor
public class CartCommand implements CommandExecutor {
    private final SkyPayments plugin;

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, String[] args) {
        if (commandSender instanceof Player) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    new CartGUI(plugin, (Player) commandSender);
                }
            }.runTaskAsynchronously(plugin);
        }
        return true;
    }
}