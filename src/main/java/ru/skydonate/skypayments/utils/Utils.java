package ru.skydonate.skypayments.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.nms.NMSUtils;
import ru.skydonate.skypayments.order.Order;
import ru.skydonate.skypayments.order.OrderStatus;

import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class Utils {
    private final SkyPayments plugin;
    @Getter
    private final NMSUtils nmsUtils;

    public String color(String from) {
        Pattern pattern = Pattern.compile("#[a-fA-F0-9]{6}");
        from = from.replace("&#", "#");
        Matcher matcher = pattern.matcher(from);
        while (matcher.find()) {
            String hexCode = from.substring(matcher.start(), matcher.end());
            String replaceSharp = hexCode.replace('#', 'x');
            char[] ch = replaceSharp.toCharArray();
            StringBuilder builder = new StringBuilder();
            for (char c : ch)
                builder.append("&").append(c);
            from = from.replace(hexCode, builder.toString());
            matcher = pattern.matcher(from);
        }

        return ChatColor.translateAlternateColorCodes('&', from);
    }

    public ItemStack makeItem(Material material, String name, LinkedList<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            itemMeta.setLore(lore);
            itemMeta.setDisplayName(name);

            itemStack.setItemMeta(itemMeta);
        }
        return itemStack;
    }

    public <T> String writeObjectInBase64(T object) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(object);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());

        } catch (Exception ignored) {
            return "";
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T readObjectFromBase64(String source, Class<T> clazz) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(source));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (T) dataInput.readObject();
        } catch (Exception ignored) {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                return null;
            }
        }
    }

    public String makeRequestAndGetAnswer(String url, String postData) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Accept", "application/json");

        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(postData);
            wr.flush();
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            return response.toString();
        }
    }

    public Order[] getActiveOrdersFromAPI() {
        String answer = "Некорректный ответ";
        try {
            answer = makeRequestAndGetAnswer("https://api-skydonate.galaxyprotect.pro/api/v1/method/orders.get", "sign=" + plugin.getMainConfig().getRequestSign());
            JsonObject jsonObject = JsonParser.parseString(answer).getAsJsonObject();

            if (jsonObject.get("success").getAsBoolean()) {
                Gson gson = new Gson();
                return gson.fromJson(jsonObject.get("response"), Order[].class);
            } else {
                throw new Exception();
            }
        } catch (Exception ignored) {
            if (plugin.getMainConfig().isDebug()) plugin.getLogger().log(Level.SEVERE, "Ответ сервера: " + answer);
        }
        return null;
    }

    public FileConfiguration loadCustomConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    public void markOrdersAsReceivedAndAdd(List<Order> orders) {
        String stringed = orders.stream()
                .map(Order::getOrder_id)
                .collect(Collectors.joining(",", "[", "]"));
        String answer = "NONE";
        boolean success = false;
        try {
            answer = makeRequestAndGetAnswer("https://api-skydonate.galaxyprotect.pro/api/v1/method/orders.received", "sign=" + plugin.getMainConfig().getRequestSign() + "&order_ids=" + stringed);
            JsonObject jsonObject = JsonParser.parseString(answer).getAsJsonObject();

            success = jsonObject.get("success").getAsBoolean();
        } catch (Exception ignored) {
        }

        if (!success) {
            if (plugin.getMainConfig().isDebug()) {
                plugin.getLogger().log(Level.SEVERE, "Не удалось пометить платежи '" + stringed + "' выполненными. Обратитесь к Администратору");
                plugin.getLogger().log(Level.SEVERE, "Ответ сервера: " + answer);
            }
        } else {
            List<String> alreadyAlerted = new ArrayList<>();
            for (Order order : orders) {
                if (order.getType().equals("command")) {
                    plugin.getDatabaseManager().getDbManager().addOrderToWaitingList(order);
                    tryReceiveCommandOrder(order);
                } else if (order.getType().equals("cart")) {
                    int counter = 1;
                    for (String action : order.getActions()) {
                        plugin.getDatabaseManager().getDbManager().addOrderToWaitingList(new Order(order.getOrder_id() + "_" + counter++, order.getType(), order.getUsername(), new String[]{action}, true, OrderStatus.WAITING_TO_RECEIVE));
                    }
                    Player receiver = Bukkit.getPlayer(order.getUsername());
                    if (receiver != null && receiver.isOnline()) {
                        if (!alreadyAlerted.contains(receiver.getName())) {
                            plugin.getMainConfig().getMessage_cartUpdated().display(receiver);
                            alreadyAlerted.add(receiver.getName());
                        }
                    }
                }
            }
        }
    }

    public void tryReceiveCommandOrder(Order order) {
        if (order.getType().equals("command")) {
            Player receiver = Bukkit.getPlayer(order.getUsername());

            if ((receiver != null && receiver.isOnline()) || !order.is_online()) {
                for (String command : order.getActions()) {
                    new BukkitRunnable() {
                        public void run() {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                        }
                    }.runTask(plugin);
                }

                plugin.getDatabaseManager().getDbManager().markOrderAsReceived(order.getUsername(), order.getOrder_id());
            }
        }
    }

    public String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void registerCommand(CommandExecutor executor, String[] aliases, String desc, String usage) {
        try {
            CMDRegister reg = new CMDRegister(aliases, desc, usage, executor, new Object(), plugin);
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            CommandMap map = (CommandMap) field.get(Bukkit.getServer());
            map.register(plugin.getDescription().getName(), reg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}