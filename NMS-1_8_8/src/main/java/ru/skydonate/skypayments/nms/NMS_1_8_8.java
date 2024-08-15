package ru.skydonate.skypayments.nms;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;

public class NMS_1_8_8 implements NMSUtils {

    public ItemStack getItemFromHand(Player player) {
        ItemStack result = player.getInventory().getItemInHand();
        if (!result.getType().equals(Material.AIR)) {
            return result;
        } else {
            return null;
        }
    }

    public boolean hasAvailableSlots(Player player, int amount) {
        for (ItemStack item : player.getInventory().getContents()) {
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
        player.sendTitle(title, subtitle);
    }

    private Class<?> getNMSClass(String name) {

        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

        try {

            return Class.forName("net.minecraft.server." + version + "." + name);

        } catch (ClassNotFoundException e) {

            e.printStackTrace();
            return null;
        }
    }

    private void sendPacket(Player player, Object packet) {

        try {

            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("playerConnection").get(handle);
            playerConnection.getClass().getMethod("sendPacket", getNMSClass("Packet")).invoke(playerConnection, packet);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public void sendActionBar(Player player, String text) {
        Class<?> chatSerializer = getNMSClass("IChatBaseComponent").getDeclaredClasses()[0];
        Class<?> chatComponent = getNMSClass("IChatBaseComponent");
        Class<?> packetActionbar = getNMSClass("PacketPlayOutChat");

        try {

            Constructor<?> ConstructorActionbar = packetActionbar.getDeclaredConstructor(chatComponent, byte.class);
            Object actionbar = chatSerializer.getMethod("a", String.class).invoke(chatSerializer, "{\"text\": \"" + text + "\"}");
            Object packet = ConstructorActionbar.newInstance(actionbar, (byte) 2);
            sendPacket(player, packet);

        } catch (Exception ex) {

            ex.printStackTrace();
        }
    }
}