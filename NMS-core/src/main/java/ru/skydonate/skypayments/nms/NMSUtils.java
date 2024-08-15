package ru.skydonate.skypayments.nms;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface NMSUtils {
    ItemStack getItemFromHand(Player player);

    boolean hasAvailableSlots(Player player, int amount);

    void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut);

    void sendActionBar(Player player, String text);
}