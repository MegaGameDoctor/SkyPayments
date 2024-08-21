package ru.skydonate.skypayments.cart;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.config.Config;
import ru.skydonate.skypayments.order.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class CartGUI {
    private final SkyPayments plugin;
    private final Player player;
    @Getter
    private final HashMap<String, List<ItemStack>> cartOrders = new HashMap<>();
    private int itemsCount;
    @Getter
    private Inventory inventory;
    @Getter
    private int invSize;
    @Getter
    private int page;

    public CartGUI(SkyPayments plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        itemsCount = 0;
        List<String> lostedItems = new ArrayList<>();
        for (Order order : plugin.getDatabaseManager().getDbManager().getActiveOrdersByType(player.getName(), "cart")) {
            List<ItemStack> items = new ArrayList<>();
            for (String itemID : order.getActions()) {
                String base64Item = plugin.getDatabaseManager().getCachedBase64Item(Integer.parseInt(itemID));
                if (!base64Item.isEmpty()) {
                    items.add(plugin.getUtils().readObjectFromBase64(base64Item, ItemStack.class));
                    itemsCount++;
                } else {
                    if (!lostedItems.contains(itemID)) lostedItems.add(itemID);
                }
            }

            if (!items.isEmpty()) {
                if (cartOrders.containsKey(order.getOrder_id())) {
                    cartOrders.get(order.getOrder_id()).addAll(items);
                } else {
                    cartOrders.put(order.getOrder_id(), items);
                }
            }
        }

        if (!lostedItems.isEmpty() && plugin.getMainConfig().isDebug()) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось отобразить корзину игроку '" + player.getName() + "'. Предметы '" + lostedItems + "' не найдены");
        }

        if (itemsCount == 0) {
            plugin.getMainConfig().getMessage_cartEmpty().display(player);
            return;
        } else if (itemsCount <= 9) {
            invSize = 9;
        } else if (itemsCount <= 18) {
            invSize = 18;
        } else if (itemsCount <= 27) {
            invSize = 27;
        } else if (itemsCount <= 36) {
            invSize = 36;
        } else if (itemsCount <= 45) {
            invSize = 45;
        } else {
            invSize = 54;
        }

        inventory = Bukkit.getServer().createInventory(null, invSize, plugin.getMainConfig().getCartInv_name());
        new BukkitRunnable() {
            @Override
            public void run() {
                open(1);
            }
        }.runTask(plugin);
    }

    public void open(int page) {
        this.page = page;
        Config cfg = plugin.getMainConfig();
        inventory.clear();
        int itemsPerPage = 45;
        int itemsToSkip = itemsPerPage * (page - 1);
        int slot = 0;
        for (List<ItemStack> itemsList : this.cartOrders.values()) {
            for (ItemStack item : itemsList) {
                if (slot + 1 > itemsPerPage) break;

                if (itemsToSkip > 0) {
                    itemsToSkip--;
                    continue;
                }

                inventory.setItem(slot, item);
                slot++;
            }
        }

        if (itemsCount > itemsPerPage * page) {
            inventory.setItem(invSize - 1, cfg.getCartInv_items_next());
        }

        if (page > 1) {
            inventory.setItem(invSize - 9, cfg.getCartInv_items_back());
        }

        player.openInventory(inventory);
        plugin.getOpenedCarts().put(player.getName(), this);
    }
}