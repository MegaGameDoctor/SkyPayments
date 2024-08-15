package ru.skydonate.skypayments.database.managers;

import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.database.DBManager;
import ru.skydonate.skypayments.order.Order;
import ru.skydonate.skypayments.order.OrderStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SQLiteDBManager implements DBManager {
    private SkyPayments plugin;
    private Connection connection;
    private String dataTableName;
    private String itemsTableName;

    public void connect(SkyPayments plugin) throws Exception {
        this.plugin = plugin;
        this.dataTableName = plugin.getConfig().getString("database.tablePrefix") + "cart";
        this.itemsTableName = plugin.getConfig().getString("database.tablePrefix") + "items";

        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite://" + plugin.getDataFolder().getAbsolutePath() + "//database.db");
        connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + dataTableName + "` (`orderID` TEXT, `serverID` INTEGER, `player` TEXT(100), `type` TEXT(30), `data` TEXT, `status` TEXT(30));").execute();
        connection.prepareStatement("CREATE TABLE IF NOT EXISTS `" + itemsTableName + "` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `item` TEXT NOT NULL);").execute();
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Order> getActiveOrdersByType(String player, String type) {
        List<Order> orders = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `" + dataTableName + "` WHERE player=? AND serverID=? AND status=? AND type=? ORDER BY orderID");
            preparedStatement.setString(1, player);
            preparedStatement.setInt(2, plugin.getMainConfig().getShop_serverID());
            preparedStatement.setString(3, OrderStatus.WAITING_TO_RECEIVE.toString());
            preparedStatement.setString(4, type);
            ResultSet set = preparedStatement.executeQuery();
            while (set.next()) {
                orders.add(new Order(set.getString("orderID"), set.getString("type"), set.getString("player"), plugin.getUtils().readObjectFromBase64(set.getString("data"), String[].class), OrderStatus.valueOf(set.getString("status"))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return orders;
    }

    public void addOrderToWaitingList(Order order) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `" + dataTableName + "` (`orderID`, `serverID`, `player`, `type`, `data`, `status`) VALUES (?, ?, ?, ?, ?, ?)");
            preparedStatement.setString(1, order.getOrder_id());
            preparedStatement.setInt(2, plugin.getMainConfig().getShop_serverID());
            preparedStatement.setString(3, order.getUsername());
            preparedStatement.setString(4, order.getType());
            preparedStatement.setString(5, plugin.getUtils().writeObjectInBase64(order.getActions()));
            preparedStatement.setString(6, order.getStatus().toString());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isAlreadyInWaitingList(String player, String orderID) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `" + dataTableName + "` WHERE orderID=? AND player=? AND serverID=? AND status=?");
            preparedStatement.setString(1, orderID);
            preparedStatement.setString(2, player);
            preparedStatement.setInt(3, plugin.getMainConfig().getShop_serverID());
            preparedStatement.setString(4, OrderStatus.WAITING_TO_RECEIVE.toString());
            ResultSet set = preparedStatement.executeQuery();
            return set.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void markOrderAsReceived(String player, String orderID) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("UPDATE `" + dataTableName + "` SET status=? WHERE ROWID = (SELECT ROWID FROM `" + dataTableName + "` WHERE orderID=? AND player=? AND serverID=? LIMIT 1);");
            preparedStatement.setString(1, OrderStatus.RECEIVED.toString());
            preparedStatement.setString(2, orderID);
            preparedStatement.setString(3, player);
            preparedStatement.setInt(4, plugin.getMainConfig().getShop_serverID());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int saveItemInBase64(String base64) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `" + itemsTableName + "` (`id`, `item`) VALUES (NULL, ?)");
            preparedStatement.setString(1, base64);
            preparedStatement.executeUpdate();

            ResultSet rs = preparedStatement.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public String getBase64Item(int id) {
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `" + itemsTableName + "` WHERE id=?");
            preparedStatement.setInt(1, id);
            ResultSet set = preparedStatement.executeQuery();
            if (set.next()) {
                return set.getString("item");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }
}