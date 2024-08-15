package ru.skydonate.skypayments;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import ru.skydonate.skypayments.cart.CartGUI;
import ru.skydonate.skypayments.order.Order;

import java.util.List;

@RequiredArgsConstructor
public class Events implements Listener {
    private final SkyPayments plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                List<Order> orders = plugin.getDatabaseManager().getDbManager().getActiveOrdersByType(player.getName(), "command");

                if (!orders.isEmpty()) {
                    for (Order order : orders) {
                        plugin.getUtils().tryReceiveCommandOrder(order);
                    }
                }
            }
        });
    }

    @EventHandler
    public void onCartClose(InventoryCloseEvent event) {
        plugin.getOpenedCarts().remove(event.getPlayer().getName());
    }

    @EventHandler
    public void onCartClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (plugin.getOpenedCarts().containsKey(player.getName())) {
            if (plugin.getOpenedCarts().get(player.getName()).getInventory().equals(event.getClickedInventory()) && event.getCurrentItem() != null) {
                ItemStack item = event.getCurrentItem();
                CartGUI cartGUI = plugin.getOpenedCarts().get(player.getName());
                if (event.getSlot() == cartGUI.getInvSize() - 1 && item.equals(plugin.getMainConfig().getCartInv_items_next())) {
                    cartGUI.open(cartGUI.getPage() + 1);
                } else if (event.getSlot() == cartGUI.getInvSize() - 9 && item.equals(plugin.getMainConfig().getCartInv_items_back())) {
                    cartGUI.open(cartGUI.getPage() - 1);
                } else if (item.getType() != null && !item.getType().equals(Material.AIR)) {
                    for (String orderID : cartGUI.getCartOrders().keySet()) {
                        List<ItemStack> order = cartGUI.getCartOrders().get(orderID);
                        if (order.contains(item)) {
                            //player.closeInventory();
                            if (plugin.getUtils().getNmsUtils().hasAvailableSlots(player, 1)) {
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        plugin.getDatabaseManager().getDbManager().markOrderAsReceived(player.getName(), orderID);
                                    }
                                }.runTaskAsynchronously(plugin);
                                player.getInventory().addItem(item);
                                cartGUI.getCartOrders().get(orderID).remove(item);
                                cartGUI.open(cartGUI.getPage());
                                plugin.getMainConfig().getMessage_cartItemsGet().display(player);
                            } else {
                                plugin.getMainConfig().getMessage_freeInventory().display(player);
                            }
                            break;
                        }
                    }
                }
            }

            event.setCancelled(true);
        }
    }
}