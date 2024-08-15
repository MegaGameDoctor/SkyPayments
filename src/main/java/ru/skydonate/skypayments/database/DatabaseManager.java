package ru.skydonate.skypayments.database;

import lombok.Getter;
import ru.skydonate.skypayments.SkyPayments;
import ru.skydonate.skypayments.database.managers.MySQLDBManager;
import ru.skydonate.skypayments.database.managers.SQLiteDBManager;

import java.util.HashMap;

@Getter
public class DatabaseManager {
    private final HashMap<Integer, String> base64ItemsCache = new HashMap<>();
    private DBManager dbManager;

    public DatabaseManager(SkyPayments plugin) throws Exception {
        switch (DBType.valueOf(plugin.getConfig().getString("database.type"))) {
            /*
            case FILE: {
                dbManager = new FileDBManager();
                break;
            }
             */
            case MYSQL: {
                dbManager = new MySQLDBManager();
                break;
            }
            case SQLITE: {
                dbManager = new SQLiteDBManager();
                break;
            }
        }

        dbManager.connect(plugin);
    }

    public String getCachedBase64Item(int id) {
        if (base64ItemsCache.containsKey(id)) {
            return base64ItemsCache.get(id);
        } else {
            String base64 = dbManager.getBase64Item(id);
            base64ItemsCache.put(id, base64);
            return base64;
        }
    }

    public int saveBase64ItemWithCache(String base64) {
        int id = dbManager.saveItemInBase64(base64);
        base64ItemsCache.put(id, base64);
        return id;
    }
}