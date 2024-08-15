package ru.skydonate.skypayments.database;

import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.order.Order;

import java.util.List;

public interface DBManager {

    void connect(SkyPayments plugin) throws Exception;

    void close();

    List<Order> getActiveOrdersByType(String player, String type);

    void addOrderToWaitingList(Order order);

    boolean isAlreadyInWaitingList(String player, String orderID);

    void markOrderAsReceived(String player, String orderID);

    int saveItemInBase64(String base64);

    String getBase64Item(int id);
}