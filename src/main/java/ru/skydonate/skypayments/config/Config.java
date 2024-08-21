package ru.skydonate.skypayments.config;

import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.commands.CartCommand;
import ru.skydonate.skypayments.utils.Utils;

import java.util.LinkedList;
import java.util.List;

@Getter
public class Config {
    private final String shop_shopID;
    private final int shop_serverID;
    private final String shop_secretKey;
    private final String requestSign;
    private final boolean debug;

    private final Message message_mainCmdHelp;
    private final TextComponent message_mainCmdEncodeAnswer;
    private final Message message_mainCmdEncodeFail;
    private final Message message_freeInventory;
    private final Message message_failed;
    private final Message message_mainCmdDecodeSuccess;
    private final Message message_cartItemsGet;
    private final Message message_cartUpdated;
    private final Message message_cartEmpty;

    private final String cartInv_name;
    private final ItemStack cartInv_items_back;
    private final ItemStack cartInv_items_next;

    public Config(SkyPayments plugin) {
        Utils utils = plugin.getUtils();
        FileConfiguration cfg = plugin.getConfig();
        FileConfiguration messagesCfg = utils.loadCustomConfig("langs/" + cfg.getString("settings.langFile"));

        this.shop_shopID = cfg.getString("shop.shopID");
        this.shop_serverID = cfg.getInt("shop.serverID");
        this.shop_secretKey = cfg.getString("shop.secretKey");
        this.requestSign = utils.md5Hash(this.shop_shopID + ":" + this.shop_serverID + ":" + this.shop_secretKey);
        this.debug = cfg.getBoolean("settings.debug", false);

        this.cartInv_name = utils.color(messagesCfg.getString("cartInv.name"));
        this.cartInv_items_next = utils.makeItem(Material.matchMaterial(messagesCfg.getString("cartInv.items.next.material")), utils.color(messagesCfg.getString("cartInv.items.next.name")), new LinkedList<>());
        this.cartInv_items_back = utils.makeItem(Material.matchMaterial(messagesCfg.getString("cartInv.items.back.material")), utils.color(messagesCfg.getString("cartInv.items.back.name")), new LinkedList<>());

        if (cfg.getBoolean("cart.command.enable", false)) {
            String cartCMDName = cfg.getString("cart.command.name");
            String[] cartCMDaliases = new String[]{cartCMDName};
            if (cfg.isSet("cart.command.aliases")) {
                List<String> aliases = cfg.getStringList("cart.command.aliases");
                if (!aliases.contains(cartCMDName)) {
                    aliases.add(cartCMDName);
                }
                cartCMDaliases = aliases.toArray(new String[0]);
            }

            utils.registerCommand(new CartCommand(plugin), cartCMDaliases, "Cart command.", "/" + cartCMDName);
        }

        this.message_mainCmdHelp = new Message(plugin, messagesCfg, "mainCmdHelp");
        this.message_mainCmdEncodeAnswer = new TextComponent(utils.color(cfg.getString("settings.messagesPrefix") + messagesCfg.getString("messages.mainCmdEncodeAnswer")));
        this.message_mainCmdEncodeFail = new Message(plugin, messagesCfg, "mainCmdEncodeFail");
        this.message_freeInventory = new Message(plugin, messagesCfg, "freeInventory");
        this.message_failed = new Message(plugin, messagesCfg, "failed");
        this.message_mainCmdDecodeSuccess = new Message(plugin, messagesCfg, "mainCmdDecodeSuccess");
        this.message_cartItemsGet = new Message(plugin, messagesCfg, "cartItemsGet");
        this.message_cartUpdated = new Message(plugin, messagesCfg, "cartUpdated");
        this.message_cartEmpty = new Message(plugin, messagesCfg, "cartEmpty");
    }
}
