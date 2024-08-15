package ru.skydonate.skypayments;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.skydonate.skypayments.cart.CartGUI;
import ru.skydonate.skypayments.commands.SkyPaymentsCommand;
import ru.skydonate.skypayments.config.Config;
import ru.skydonate.skypayments.database.DatabaseManager;
import ru.skydonate.skypayments.nms.NMSUtils;
import ru.skydonate.skypayments.nms.NMS_1_13_2;
import ru.skydonate.skypayments.nms.NMS_1_8_8;
import ru.skydonate.skypayments.order.Order;
import ru.skydonate.skypayments.order.OrderStatus;
import ru.skydonate.skypayments.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

@Getter
public class SkyPayments extends JavaPlugin {
    private final HashMap<String, CartGUI> openedCarts = new HashMap<>();
    private Utils utils;
    private Config mainConfig;
    private DatabaseManager databaseManager;
    private Thread mainChecker;

    public void onEnable() {
        this.getLogger().log(Level.INFO, "Запускаю плагин...");
        long start = System.currentTimeMillis();
        this.saveDefaultConfig();
        utils = new Utils(this, getActualNMS());
        this.getLogger().log(Level.INFO, "Обрабатываю конфигурацию...");
        mainConfig = new Config(this);
        this.getLogger().log(Level.INFO, "Подключаюсь к БД...");
        try {
            databaseManager = new DatabaseManager(this);
        } catch (Exception exception) {
            exception.printStackTrace();
            this.getLogger().log(Level.SEVERE, "Не удалось подключиться к БД. Запуск отменён");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        this.getLogger().log(Level.INFO, "Выполняю другие процессы...");
        Bukkit.getPluginManager().registerEvents(new Events(this), this);

        SkyPaymentsCommand skyPaymentsCommand = new SkyPaymentsCommand(this);
        this.getCommand("skypayments").setExecutor(skyPaymentsCommand);
        this.getCommand("skypayments").setTabCompleter(skyPaymentsCommand);

        startCheckRunnable();

        this.getLogger().log(Level.INFO, "Полностью запущен! (" + (System.currentTimeMillis() - start) + " мс)");
    }

    private NMSUtils getActualNMS() {
        String version = Bukkit.getVersion().split("\\(MC: ")[1].replace(")", "");

        switch (version) {
            case "1.8":
            case "1.8.8":
            case "1.8.9":
            case "1.9":
            case "1.9.1":
            case "1.9.2":
            case "1.9.3":
            case "1.9.4":
                return new NMS_1_8_8();
            default:
                return new NMS_1_13_2();
        }
    }

    private void startCheckRunnable() {
        int periodSeconds = 15;
        mainChecker = new Thread() {
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    Order[] orders = utils.getActiveOrdersFromAPI();
                    if (orders != null) {
                        List<Order> toMarkReceivedAndAdd = new ArrayList<>();

                        for (Order order : orders) {
                            if (!databaseManager.getDbManager().isAlreadyInWaitingList(order.getUsername(), String.valueOf(order.getOrder_id()))) {
                                order.setStatus(OrderStatus.WAITING_TO_RECEIVE);

                                toMarkReceivedAndAdd.add(order);
                            }
                        }

                        if (!toMarkReceivedAndAdd.isEmpty()) {
                            utils.markOrdersAsReceivedAndAdd(toMarkReceivedAndAdd);
                        }
                    } else {
                        if (mainChecker != null)
                            getLogger().log(Level.SEVERE, "Не удалось получить список платежей. Обратитесь к Администратору");
                    }
                    try {
                        Thread.sleep(1000L * periodSeconds);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        };
        mainChecker.start();
    }

    public void onDisable() {
        this.getLogger().log(Level.INFO, "Выключаю плагин...");
        Bukkit.getScheduler().cancelTasks(this);

        if (mainChecker != null) {
            mainChecker.interrupt();
            mainChecker = null;
        }

        if (databaseManager.getDbManager() != null) databaseManager.getDbManager().close();
        this.getLogger().log(Level.INFO, "Плагин успешно выключен!");
    }
}