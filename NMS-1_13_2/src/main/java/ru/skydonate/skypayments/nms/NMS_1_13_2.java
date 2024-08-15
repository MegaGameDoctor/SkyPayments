package ru.skydonate.skypayments.nms;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class NMS_1_13_2 implements NMSUtils {

    public ItemStack getItemFromHand(Player player) {
        ItemStack result = player.getInventory().getItemInMainHand();
        if (!result.getType().equals(Material.AIR)) {
            return result;
        } else {
            return null;
        }
    }

    public boolean hasAvailableSlots(Player player, int amount) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType().equals(Material.AIR)) {
                amount--;
                if (amount == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    public void sendTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void sendActionBar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(text));
    }
}